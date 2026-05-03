package com.auction.client.network;

import com.auction.client.RealtimeAuctionBus;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
            socket = new Socket(serverHost, serverPort);

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
            // #region agent log
            try {
                Path log = Paths.get(System.getProperty("user.dir")).normalize().resolve("debug-438ab6.log");
                String msg = String.valueOf(e.getMessage()).replace("\\", "/").replace("\"", "'");
                String line =
                        "{\"sessionId\":\"438ab6\",\"hypothesisId\":\"CONNECT_REFUSED\",\"location\":\"SocketClient.connect\""
                                + ",\"message\":\"socket connect failed\",\"data\":{\"host\":\"" + serverHost.replace("\"", "'")
                                + "\",\"port\":"
                                + serverPort + ",\"err\":\"" + msg + "\"},\"timestamp\":"
                                + System.currentTimeMillis()
                                + "}\n";
                Files.writeString(log, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {
            }
            // #endregion
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
                Message incoming = (Message) objectInputStream.readObject();
                dispatchIncoming(incoming);
            }
        } catch (EOFException e) {
            System.out.println("✓ Server closed connection");
        } catch (IOException | ClassNotFoundException e) {
            if (!stopped) {
                System.err.println("✗ Socket reader: " + e.getMessage());
            }
        } finally {
            isConnected = false;
            failAllPending(new IOException("Connection closed"));
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
        if (!isConnected) {
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
            return null;
        } catch (TimeoutException e) {
            pendingRequests.remove(cid);
            System.err.println("✗ RPC timeout");
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingRequests.remove(cid);
            return null;
        } catch (ExecutionException e) {
            pendingRequests.remove(cid);
            System.err.println("✗ RPC failed: " + e.getCause().getMessage());
            return null;
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
