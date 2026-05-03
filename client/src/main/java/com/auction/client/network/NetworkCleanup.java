package com.auction.client.network;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.SessionContext;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

/** Đóng socket, xóa session và realtime listeners (logout / thoát app). */
public final class NetworkCleanup {

    private NetworkCleanup() {
    }

    /** Tên ngắn gọn (alias) để tái khai báo giống {@link #logoutClient()}. */
    public static void cleanup() {
        logoutClient();
    }

    /**
     * Gửi {@link MessageType#LOGOUT_REQUEST} nếu đang kết nối (server có thể ghi log),
     * sau đó ngắt socket và dọn client-side state.
     */
    public static void logoutClient() {
        ClientConnection conn = ClientConnection.getInstance();
        if (conn.isConnected()) {
            SocketClient client = conn.getSocketClient();
            if (client != null) {
                client.sendMessage(new Message(MessageType.LOGOUT_REQUEST, null));
            }
        }
        RealtimeAuctionBus.clearAllListeners();
        SessionContext.clear();
        conn.disconnect();
    }
}
