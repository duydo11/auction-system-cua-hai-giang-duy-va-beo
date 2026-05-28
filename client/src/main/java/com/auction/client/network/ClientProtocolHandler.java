package com.auction.client.network;

import com.auction.client.util.AuctionCache;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AutoBidConfig;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.User;
import com.auction.shared.model.user.Transaction;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RPC qua socket ({@link Message}): dùng cho JavaFX / controller.
 */
public class ClientProtocolHandler {
    private static final Logger logger = Logger.getLogger(ClientProtocolHandler.class.getName());

    private final ClientConnection connection;
    private static final Object RPC_LOCK = new Object();
    private volatile String lastTransportError;

    public ClientProtocolHandler() {
        this(ClientConnection.getInstance());
    }

    public ClientProtocolHandler(ClientConnection connection) {
        this.connection = connection;
    }

    private Message send(MessageType type, Object payload) {
        synchronized (RPC_LOCK) {
            // Nhiều controller dùng chung singleton socket; serialize RPC để request này không đóng socket của request khác.
            lastTransportError = null;
            Message request = new Message(type, payload);
            Message response = sendOnce(request);
            if (response != null) {
                return response;
            }

            // Retry một lần sau khi reconnect để xử lý socket cũ bị server/client đóng.
            connection.disconnect();
            if (!connection.connect()) {
                lastTransportError = "Cannot connect to server " + connection.getHost() + ":" + connection.getPort();
                logger.warning("✗ " + lastTransportError);
                return null;
            }
            response = sendOnce(request);
            if (response == null) {
                lastTransportError = "Connection lost or timed out while sending " + type;
                logger.warning("✗ " + lastTransportError);
            }
            return response;
        }
    }

    private Message sendOnce(Message request) {
        if (!connection.isConnected() && !connection.connect()) {
            lastTransportError = "Server is not ready or is currently offline.";
            return null;
        }
        SocketClient client = connection.getSocketClient();
        if (client == null) {
            lastTransportError = "Socket client has not been initialized.";
            return null;
        }
        return client.sendMessage(request);
    }

    // ========================
    // Auth methods
    // ========================

    public User login(String username, String password) {
        Message response = send(MessageType.LOGIN_REQUEST, new String[]{username, password});
        if (response == null) {
            return null;
        }
        if (!response.isSuccess()) {
            // Giữ nguyên lỗi từ server, ví dụ: "bạn đã bị admin ban".
            lastTransportError = lastError(response);
            return null;
        }
        Object data = response.getData();
        return data instanceof User ? (User) data : null;
    }

    public boolean register(String username, String password, String email, String role) {
        Message response = send(MessageType.REGISTER_REQUEST, new String[]{username, password, email, role});
        return response != null && response.isSuccess();
    }

    /** @return {@code null} nếu đăng ký thành công, ngược lại là thông báo lỗi */
    public String registerOrError(String username, String password, String email) {
        Message response = send(MessageType.REGISTER_REQUEST, new String[]{username, password, email});
        if (response != null && response.isSuccess()) {
            return null;
        }
        return lastError(response);
    }

    // ========================
    // Auction methods
    // ========================

    @SuppressWarnings("unchecked")
    public List<AuctionSession> getActiveAuctions() {
        Message response = send(MessageType.VIEW_AUCTIONS_REQUEST, null);
        if (response == null || !response.isSuccess()) {
            // Lỗi mạng tạm thời thì giữ lại active cache, không trả nhầm all/history.
            return AuctionCache.getActive();
        }
        Object data = response.getData();
        if (data instanceof List<?> list && list.isEmpty()) {
            AuctionCache.updateActive(Collections.emptyList());
            return Collections.emptyList();
        }
        if (data instanceof List<?> list && list.get(0) instanceof AuctionSession) {
            List<AuctionSession> auctions = (List<AuctionSession>) data;
            AuctionCache.updateActive(auctions);
            return auctions;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<AuctionSession> getAllAuctions() {
        Message response = send(MessageType.GET_ALL_AUCTIONS_REQUEST, null);
        if (response == null || !response.isSuccess()) {
            // Lỗi mạng tạm thời thì giữ lại all cache để My Listings/admin không bị trống.
            return AuctionCache.getAll();
        }
        Object data = response.getData();
        if (data instanceof List<?> list && list.isEmpty()) {
            AuctionCache.updateAll(Collections.emptyList());
            return Collections.emptyList();
        }
        if (data instanceof List<?> list && list.get(0) instanceof AuctionSession) {
            List<AuctionSession> auctions = (List<AuctionSession>) data;
            AuctionCache.updateAll(auctions);
            return auctions;
        }
        return Collections.emptyList();
    }

    public boolean createAuction(AuctionSession auction) {
        Message response = send(MessageType.CREATE_AUCTION_REQUEST, auction);
        return response != null && response.isSuccess();
    }

    /** @return {@code null} nếu tạo phiên thành công */
    public String createAuctionOrError(AuctionSession auction) {
        Message response = send(MessageType.CREATE_AUCTION_REQUEST, auction);
        if (response != null && response.isSuccess()) {
            return null;
        }
        return lastError(response);
    }

    // ========================
    // Bidding methods
    // ========================

    public boolean placeBid(int sessionId, int bidderId, double bidAmount) {
        Message response = send(
                MessageType.PLACE_BID_REQUEST,
                new String[]{String.valueOf(sessionId), String.valueOf(bidderId), String.valueOf(bidAmount)}
        );
        return response != null && response.isSuccess();
    }

    /** @return {@code null} nếu đặt giá thành công */
    public String placeBidOrError(int sessionId, int bidderId, double bidAmount) {
        Message response = send(
                MessageType.PLACE_BID_REQUEST,
                new String[]{String.valueOf(sessionId), String.valueOf(bidderId), String.valueOf(bidAmount)}
        );
        if (response != null && response.isSuccess()) {
            return null;
        }
        return lastError(response);
    }

    @SuppressWarnings("unchecked")
    public List<Bid> getBidHistory(int sessionId) {
        Message response = send(MessageType.GET_BIDS_REQUEST, String.valueOf(sessionId));
        if (response == null || !response.isSuccess()) {
            return Collections.emptyList();
        }
        Object data = response.getData();
        if (data instanceof List<?> list && list.isEmpty()) {
            return Collections.emptyList();
        }
        if (data instanceof List<?> list && list.get(0) instanceof Bid) {
            return (List<Bid>) data;
        }
        return Collections.emptyList();
    }

    // ========================
    // Item Management methods
    // ========================

    /**
     * Sửa thông tin item.
     * @return true nếu thành công
     */
    public boolean updateItem(Item item) {
        Message response = send(MessageType.UPDATE_ITEM_REQUEST, item);
        return response != null && response.isSuccess();
    }

    /**
     * Sửa thông tin item.
     * @return null nếu thành công, error message nếu thất bại
     */
    public String updateItemOrError(Item item) {
        Message response = send(MessageType.UPDATE_ITEM_REQUEST, item);
        if (response != null && response.isSuccess()) return null;
        return lastError(response);
    }

    /**
     * Xóa item theo id.
     * @return true nếu thành công
     */
    public boolean deleteItem(int itemId) {
        Message response = send(MessageType.DELETE_ITEM_REQUEST, String.valueOf(itemId));
        if (response != null && response.isSuccess()) {
            AuctionCache.removeByItemId(itemId);
            return true;
        }
        return false;
    }

    /**
     * Xóa item theo id.
     * @return null nếu thành công, error message nếu thất bại
     */
    public String deleteItemOrError(int itemId) {
        Message response = send(MessageType.DELETE_ITEM_REQUEST, String.valueOf(itemId));
        if (response != null && response.isSuccess()) {
            AuctionCache.removeByItemId(itemId);
            return null;
        }
        return lastError(response);
    }

    // ========================
    // Auto-Bidding methods
    // ========================

    /**
     * Đăng ký auto-bid cho một phiên.
     * @return true nếu thành công
     */
    public boolean registerAutoBid(AutoBidConfig config) {
        Message response = send(MessageType.REGISTER_AUTO_BID_REQUEST, config);
        return response != null && response.isSuccess();
    }

    /**
     * Hủy auto-bid.
     * @return true nếu thành công
     */
    public boolean cancelAutoBid(int sessionId, int bidderId) {
        Message response = send(
                MessageType.CANCEL_AUTO_BID_REQUEST,
                new int[]{sessionId, bidderId}
        );
        return response != null && response.isSuccess();
    }

    // ========================
    // Admin methods
    // ========================

    /**
     * Ban user (chỉ admin).
     * @return true nếu thành công
     */
    public boolean banUser(int userId) {
        Message response = send(MessageType.BAN_USER_REQUEST, String.valueOf(userId));
        return response != null && response.isSuccess();
    }

    /**
     * Lấy danh sách tất cả user (chỉ admin).
     * @return danh sách user, empty list nếu thất bại
     */
    @SuppressWarnings("unchecked")
    public List<User> getAllUsers() {
        Message response = send(MessageType.GET_ALL_USERS_REQUEST, null);
        if (response == null || !response.isSuccess()) {
            return Collections.emptyList();
        }
        Object data = response.getData();
        if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof User) {
            return (List<User>) data;
        }
        return Collections.emptyList();
    }

    // ========================
    // User & Wallet methods
    // ========================

    public User getUserInfo(int userId) {
        Message response = send(MessageType.GET_USER_INFO_REQUEST, String.valueOf(userId));
        if (response == null || !response.isSuccess()) {
            return null;
        }
        Object data = response.getData();
        return data instanceof User ? (User) data : null;
    }

    public boolean updateUser(User user) {
        Message response = send(MessageType.UPDATE_USER_REQUEST, user);
        return response != null && response.isSuccess();
    }

    /**
     * Persist a wallet deposit/withdraw entry after the balance update succeeds.
     * The server stores this as a transaction so Wallet1/Wallet2 can render it later.
     */
    public boolean saveTransaction(Transaction transaction) {
        Message response = send(MessageType.SAVE_TRANSACTION_REQUEST, transaction);
        return response != null && response.isSuccess();
    }

    /** Convenience helper used by DepositActionController. */
    public boolean deposit(User user, double amount) {
        if (user == null || amount <= 0) return false;
        // Lấy user mới nhất từ server rồi mới cộng tiền, tránh cộng local trước làm UI/DB lệch nhau.
        User latest = getUserInfo(user.getId());
        if (latest == null) latest = user;
        if (latest instanceof com.auction.shared.model.user.Bidder bidder) {
            bidder.setAccountBalance(bidder.getAccountBalance() + amount);
        } else if (latest instanceof com.auction.shared.model.user.Seller seller) {
            seller.setAccountBalance(seller.getAccountBalance() + amount);
        }
        boolean updated = updateUser(latest);
        if (!updated) return false;
        return saveTransaction(new Transaction(
                0,
                latest.getId(),
                amount,
                "DEPOSIT",
                "Deposit",
                LocalDateTime.now()
        ));
    }

    /** Convenience helper used by WithdrawActionController. */
    public boolean withdraw(User user, double amount) {
        if (user == null || amount <= 0) return false;
        boolean updated = updateUser(user);
        if (!updated) return false;
        return saveTransaction(new Transaction(
                0,
                user.getId(),
                -amount,
                "WITHDRAW",
                "Withdraw",
                LocalDateTime.now()
        ));
    }

    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactions(int userId) {
        Message response = send(MessageType.GET_TRANSACTIONS_REQUEST, String.valueOf(userId));
        if (response == null || !response.isSuccess()) {
            return Collections.emptyList();
        }
        Object data = response.getData();
        if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Transaction) {
            return (List<Transaction>) data;
        }
        return Collections.emptyList();
    }

    // ========================
    // Utilities
    // ========================

    public String lastError(Message response) {
        if (response == null) {
            return lastTransportError != null ? lastTransportError : "No response from server (connection lost?)";
        }
        if (response.getErrorMessage() != null) {
            return response.getErrorMessage();
        }
        if (!response.isSuccess() && response.getData() != null) {
            return String.valueOf(response.getData());
        }
        return "";
    }

    public String getLastTransportError() {
        return lastTransportError;
    }
    // Thêm vào trong class ClientProtocolHandler
    private boolean sendRequest(String command, Object data) {
        try {
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancelAuction(int sessionId) {
        return sendRequest("CANCEL_AUCTION", sessionId);
    }

    public AuctionSession getAuctionDetail(int id) {
        Object response = sendRequest("GET_AUCTION_DETAIL", id);

        if (response instanceof AuctionSession) {
            return (AuctionSession) response;
        }

        return null;
    }
}
