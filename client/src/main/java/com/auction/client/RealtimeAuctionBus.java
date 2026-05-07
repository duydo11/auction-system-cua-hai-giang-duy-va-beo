package com.auction.client;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.protocol.Message;
import javafx.application.Platform;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Push từ server ({@code AUCTION_*_PUSH}) → UI JavaFX.
 * <p>Mô hình Observer: controller/UI {@link #addAuctionListener} / {@link #removeAuctionListener};
 * luồng socket không đụng trực tiếp JavaFX — {@link javafx.application.Platform#runLater} trong {@link #dispatch}.</p>
 */
public final class RealtimeAuctionBus {

    private static final CopyOnWriteArrayList<Consumer<AuctionSession>> listeners = new CopyOnWriteArrayList<>();

    private RealtimeAuctionBus() {
    }

    public static void addAuctionListener(Consumer<AuctionSession> listener) {
        listeners.add(listener);
    }

    public static void removeAuctionListener(Consumer<AuctionSession> listener) {
        listeners.remove(listener);
    }

    /** Gọi khi logout / đóng app để tránh listener cũ chạy trên scene đã bỏ. */
    public static void clearAllListeners() {
        listeners.clear();
    }

    public static void dispatch(Message message) {
        if (!(message.getData() instanceof AuctionSession session)) {
            return;
        }
        for (Consumer<AuctionSession> listener : listeners) {
            Platform.runLater(() -> listener.accept(session));
        }
    }
}
