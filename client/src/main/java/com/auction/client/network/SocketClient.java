package com.auction.client.network;

import com.auction.client.RealtimeAuctionBus;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Một luồng đọc liên tục: phản hồi RPC theo {@code correlationId}, push realtime giao cho {@link RealtimeAuctionBus}.
 */
public class SocketClient {
    private static final long RPC_TIMEOUT_SEC = 60;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;

    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private volatile boolean isConnected;
    private volatile boolean stopped;
    private Thread readerThread;
    private final Object writeLock = new Object();
    private final ConcurrentHashMap<String, CompletableFuture<Message>> pendingRequests = new ConcurrentHashMap<>();

    public SocketClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public boolean connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverHost, serverPort), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            socket.setKeepAlive(true);

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(socket.getInputStream());

            isConnected = true;
            stopped = false;
            startReader();

            System.out.println("✓ Connected to server: " + serverHost + ":" + serverPort);
            return true;
        } catch (IOException e) {
            System.err.println("✗ Failed to connect to server: " + e.getMessage());
            if (e instanceof ConnectException
                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("connection refused"))) {
                System.err.println("→ Kiểm tra: (1) Đã Run ServerMain / SocketServer và thấy \"Listening TCP\"? (2) Đồng port "
                        + serverPort + " với server/src/main/resources/config.properties và client.properties? "
                        + "(3) Firewall/antivirus không chặn Java?");
            }
            return false;
        }
    }

    private void startReader() {
        readerThread = new Thread(this::readLoop, "auction-socket-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try {
            while (!stopped && isConnected) {
                try {
                    Message incoming = (Message) objectInputStream.readObject();
                    dispatchIncoming(incoming);
                } catch (SocketTimeoutException e) {
                    // Keep reader alive; timeout helps detect dead connections periodically.
                }
            }
        } catch (EOFException e) {
            System.out.println("✓ Server closed connection");
        } catch (IOException | ClassNotFoundException e) {
            if (!stopped) {
                System.err.println("✗ Socket reader: " + e.getMessage());
            }
        } finally {
            handleConnectionLoss(new IOException("Connection closed by reader loop"));
        }
    }

    private void dispatchIncoming(Message m) {
        String cid = m.getCorrelationId();
        if (cid != null && !cid.isBlank()) {
            CompletableFuture<Message> fut = pendingRequests.remove(cid);
            if (fut != null) {
                fut.complete(m);
                return;
            }
        }
        if (m.getType() == MessageType.AUCTION_UPDATED_PUSH || m.getType() == MessageType.AUCTION_CREATED_PUSH) {
            RealtimeAuctionBus.dispatch(m);
        }
    }

    private void failAllPending(Exception reason) {
        pendingRequests.values().forEach(f -> f.completeExceptionally(reason));
        pendingRequests.clear();
    }

    public Message sendMessage(Message message) {
        if (!isConnected || objectOutputStream == null) {
            System.err.println("✗ Not connected to server");
            return null;
        }

        String cid = message.getCorrelationId();
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
            message.setCorrelationId(cid);
        }

        CompletableFuture<Message> fut = new CompletableFuture<>();
        pendingRequests.put(cid, fut);

        try {
            synchronized (writeLock) {
                System.out.println("→ Sending to server: " + message.getType());
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
            }

            Message response = fut.get(RPC_TIMEOUT_SEC, TimeUnit.SECONDS);
            System.out.println("← Received from server: " + response.getType());
            return response;
        } catch (IOException e) {
            pendingRequests.remove(cid);
            System.err.println("✗ Socket write failed: " + e.getMessage());
            handleConnectionLoss(e);
            return null;
        } catch (TimeoutException e) {
            pendingRequests.remove(cid);
            System.err.println("✗ RPC timeout (" + RPC_TIMEOUT_SEC + "s) for: " + message.getType());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingRequests.remove(cid);
            return null;
        } catch (ExecutionException e) {
            pendingRequests.remove(cid);
            Throwable cause = e.getCause();
            System.err.println("✗ RPC failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
            return null;
        }
    }

    private void handleConnectionLoss(Exception reason) {
        isConnected = false;
        stopped = true;
        failAllPending(new IOException("Connection lost", reason));
        try {
            if (objectInputStream != null) {
                objectInputStream.close();
            }
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException closeError) {
            System.err.println("✗ Error closing lost connection: " + closeError.getMessage());
        }
    }

    public void disconnect() {
        stopped = true;
        isConnected = false;
        failAllPending(new IOException("Disconnected"));
        try {
            if (objectInputStream != null) objectInputStream.close();
            if (objectOutputStream != null) objectOutputStream.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("✓ Disconnected from server");
        } catch (IOException e) {
            System.err.println("✗ Error disconnecting: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
