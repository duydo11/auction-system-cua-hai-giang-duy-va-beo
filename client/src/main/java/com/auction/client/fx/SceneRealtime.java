package com.auction.client.fx;

import com.auction.client.RealtimeAuctionBus;
import com.auction.shared.model.auction.AuctionSession;
import javafx.scene.Node;

import java.util.function.Consumer;

/**
 * Gắn/gỡ {@link RealtimeAuctionBus} theo vòng đời scene — khi đổi màn (scene = null) không còn nhận push.
 */
public final class SceneRealtime {

    private SceneRealtime() {
    }

    public static void attachAuctionUpdates(Node anchor, Consumer<AuctionSession> listener) {
        anchor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                RealtimeAuctionBus.removeAuctionListener(listener);
            }
            if (newScene != null) {
                RealtimeAuctionBus.addAuctionListener(listener);
            }
        });
        if (anchor.getScene() != null) {
            RealtimeAuctionBus.addAuctionListener(listener);
        }
    }
}
