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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Một luồng đọc liên tục: phản hồi RPC theo {@code correlationId}, push realtime giao cho {@link RealtimeAuctionBus}.
 */
public class SocketClient {
    private static final Logger logger = Logger.getLogger(SocketClient.class.getName());
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

            logger.info("Connected to server: " + serverHost + ":" + serverPort);
            return true;
        } catch (IOException e) {
            logger.warning("Failed to connect to server: " + e.getMessage());
            if (e instanceof ConnectException
                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("connection refused"))) {
                logger.warning("Kiểm tra: (1) Đã Run ServerMain / SocketServer? (2) Đúng port "
                        + serverPort + "? (3) Firewall không chặn Java?");
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
            logger.info("Server closed connection");
        } catch (IOException | ClassNotFoundException e) {
            if (!stopped) {
                logger.log(Level.WARNING, "Socket reader error", e);
            }
        } finally {
            handleConnectionLoss(new IOException("Connection closed by reader loop"));
        }
    }

    private void dispatchIncoming(Message m) {
        String cid = m.getCorrelationId();
        if (cid != null && !cid.trim().isEmpty()) {
            CompletableFuture<Message> fut = pendingRequests.remove(cid);
            if (fut != null) {
                fut.complete(m);
                return;
            }
        }
        // Server push → dispatch to RealtimeAuctionBus or handle special pushes
        MessageType type = m.getType();
        if (type == MessageType.AUCTION_UPDATED_PUSH
                || type == MessageType.AUCTION_CREATED_PUSH
                || type == MessageType.CLOSE_AUCTION_PUSH
                || type == MessageType.AUCTION_EXTENDED_PUSH) {
            RealtimeAuctionBus.dispatch(m);
        } else if (type == MessageType.USER_BANNED_PUSH) {
            handleUserBannedPush(m);
        }
    }

    /**
     * Xử lý push khi user bị ban: nếu là current user thì logout và về login screen.
     */
    private void handleUserBannedPush(Message m) {
        try {
            int bannedUserId = (Integer) m.getData();
            com.auction.shared.model.user.User currentUser = com.auction.client.SessionContext.getCurrentUser();
            if (currentUser != null && currentUser.getId() == bannedUserId) {
                javafx.application.Platform.runLater(() -> {
                    com.auction.client.SessionContext.setCurrentUser(null);
                    com.auction.client.util.SceneNavigator.loadScene(
                            com.auction.client.util.SceneNavigator.LOGIN, "login");
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Account Banned");
                    alert.setHeaderText("Your account has been banned");
                    alert.setContentText("Your account has been banned by an administrator. Please contact support.");
                    alert.show();
                });
            }
        } catch (Exception e) {
            logger.warning("Error handling USER_BANNED_PUSH: " + e.getMessage());
        }
    }

    private void failAllPending(Exception reason) {
        pendingRequests.values().forEach(f -> f.completeExceptionally(reason));
        pendingRequests.clear();
    }

    public Message sendMessage(Message message) {
        if (!isConnected || objectOutputStream == null) {
            logger.warning("Not connected to server");
            return null;
        }

        String cid = message.getCorrelationId();
        if (cid == null || cid.trim().isEmpty()) {
            cid = UUID.randomUUID().toString();
            message.setCorrelationId(cid);
        }

        CompletableFuture<Message> fut = new CompletableFuture<>();
        pendingRequests.put(cid, fut);

        try {
            synchronized (writeLock) {
                logger.fine("Sending to server: " + message.getType());
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
            }

            Message response = fut.get(RPC_TIMEOUT_SEC, TimeUnit.SECONDS);
            logger.fine("Response received: " + response.getType());
            return response;
        } catch (IOException e) {
            pendingRequests.remove(cid);
            logger.log(Level.WARNING, "Socket write failed: " + e.getMessage(), e);
            handleConnectionLoss(e);
            return null;
        } catch (TimeoutException e) {
            pendingRequests.remove(cid);
            logger.warning("RPC timeout (" + RPC_TIMEOUT_SEC + "s) for: " + message.getType());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingRequests.remove(cid);
            return null;
        } catch (ExecutionException e) {
            pendingRequests.remove(cid);
            Throwable cause = e.getCause();
            logger.warning("RPC failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
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
            logger.log(Level.FINE, "Error closing lost connection", closeError);
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
            logger.info("Disconnected from server");
        } catch (IOException e) {
            logger.log(Level.FINE, "Error disconnecting", e);
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
