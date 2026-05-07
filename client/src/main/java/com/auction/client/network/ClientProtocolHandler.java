package com.auction.client.network;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.util.Collections;
import java.util.List;

/**
 * RPC qua socket ({@link Message}): dùng cho JavaFX / controller.
 */
public class ClientProtocolHandler {
    private final ClientConnection connection;
    private volatile String lastTransportError;

    public ClientProtocolHandler() {
        this(ClientConnection.getInstance());
    }

    public ClientProtocolHandler(ClientConnection connection) {
        this.connection = connection;
    }

    private Message send(MessageType type, Object payload) {
        lastTransportError = null;
        Message request = new Message(type, payload);
        Message response = sendOnce(request);
        if (response != null) {
            return response;
        }

        // Retry once after reconnect to avoid transient socket drop breaking the UI flow.
        connection.disconnect();
        if (!connection.connect()) {
            lastTransportError = "Không thể kết nối server " + connection.getHost() + ":" + connection.getPort();
            System.err.println("✗ " + lastTransportError);
            return null;
        }
        response = sendOnce(request);
        if (response == null) {
            lastTransportError = "Mất kết nối hoặc timeout khi gửi " + type;
            System.err.println("✗ " + lastTransportError);
        }
        return response;
    }

    private Message sendOnce(Message request) {
        if (!connection.isConnected() && !connection.connect()) {
            lastTransportError = "Server chưa sẵn sàng hoặc đang tắt.";
            return null;
        }
        SocketClient client = connection.getSocketClient();
        if (client == null) {
            lastTransportError = "Socket client chưa khởi tạo.";
            return null;
        }
        return client.sendMessage(request);
    }

    public User login(String username, String password) {
        Message response = send(MessageType.LOGIN_REQUEST, new String[]{username, password});
        if (response == null || !response.isSuccess()) {
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
    public String registerOrError(String username, String password, String email, String role) {
        Message response = send(MessageType.REGISTER_REQUEST, new String[]{username, password, email, role});
        if (response != null && response.isSuccess()) {
            return null;
        }
        return lastError(response);
    }

    @SuppressWarnings("unchecked")
    public List<AuctionSession> getActiveAuctions() {
        Message response = send(MessageType.VIEW_AUCTIONS_REQUEST, null);
        if (response == null || !response.isSuccess()) {
            return Collections.emptyList();
        }
        Object data = response.getData();
        if (data instanceof List<?> list && list.isEmpty()) {
            return Collections.emptyList();
        }
        if (data instanceof List<?> list && list.get(0) instanceof AuctionSession) {
            return (List<AuctionSession>) data;
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

    public String lastError(Message response) {
        if (response == null) {
            return lastTransportError != null ? lastTransportError : "Không có phản hồi từ server (mất kết nối?)";
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
}
