package com.auction.client.controller.BidderScene;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Dashboard cho bidder.
 */
public class BidderDashboardController implements Initializable {

    @FXML private Label lblUsername;
    @FXML private HBox containerTopPicks;
    @FXML private HBox containerEndingSoon;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private final Map<Integer, ProductCardController> cardControllers = new HashMap<>();
    private Consumer<AuctionSession> realtimeListener;
    private Timeline refreshTimer;
    private boolean isLoadingActive;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionContext.getCurrentUser() != null) {
            lblUsername.setText(SessionContext.getCurrentUser().getUsername());
        } else {
            lblUsername.setText("Guest User");
        }

        loadActiveAuctionsAsync();
        setupRealtimeListener();
        startRefreshTimer();
    }

    private void loadActiveAuctionsAsync() {
        if (isLoadingActive) {
            return;
        }
        isLoadingActive = true;
        if (AuctionCache.hasActiveData()) {
            renderAuctions(AuctionCache.getActive());
        } else {
            clearContainers();
            cardControllers.clear();
            showDashboardMessage("Loading active auctions...", "#757575");
        }

        // Luôn refresh nền, không chờ TTL. Đây là lưới an toàn nếu push realtime bị miss.
        FxAsync.run("bidder-dashboard-load", protocol::getActiveAuctions,
                auctions -> {
                    isLoadingActive = false;
                    renderAuctions(auctions);
                },
                error -> {
                    isLoadingActive = false;
                    if (!AuctionCache.hasActiveData()) {
                        showDashboardMessage("Could not connect to the server.", "#c62828");
                    }
                });
    }

    private void renderAuctions(List<AuctionSession> auctions) {
        clearContainers();
        cardControllers.clear();

        List<AuctionSession> active = auctions.stream()
                .filter(this::isBiddableNow)
                .toList();
        if (active.isEmpty()) {
            showDashboardMessage("No active auctions are available yet.", "#757575");
            return;
        }

        for (AuctionSession session : active) {
            loadProductCard(session, containerTopPicks);
        }

        active.stream()
                .sorted(Comparator.comparing(AuctionSession::getEndTime))
                .limit(4)
                .forEach(session -> loadProductCard(session, containerEndingSoon));
    }

    private boolean isBiddableNow(AuctionSession session) {
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        com.auction.shared.model.auction.AuctionStatus status = session.getStatus();
        return !now.isBefore(session.getStartTime()) && now.isBefore(session.getEndTime())
                && status != com.auction.shared.model.auction.AuctionStatus.CANCELED
                && status != com.auction.shared.model.auction.AuctionStatus.FINISHED
                && status != com.auction.shared.model.auction.AuctionStatus.PAID;
    }

    private void clearContainers() {
        if (containerTopPicks != null) {
            containerTopPicks.getChildren().clear();
        }
        if (containerEndingSoon != null) {
            containerEndingSoon.getChildren().clear();
        }
    }

    private void showDashboardMessage(String message, String color) {
        HBox targetContainer = containerTopPicks != null ? containerTopPicks : containerEndingSoon;
        if (targetContainer == null) {
            System.err.println("Bidder dashboard FXML containers are not injected.");
            return;
        }
        targetContainer.getChildren().clear();
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: " + color + ";");
        targetContainer.getChildren().add(label);
    }

    private void loadProductCard(AuctionSession session, HBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Card/ProductCard.fxml")
            );
            Node card = loader.load();

            ProductCardController controller = loader.getController();
            controller.setAuctionSession(session);
            cardControllers.put(session.getId(), controller);

            card.getStyleClass().add("product-card");
            container.getChildren().add(card);

        } catch (IOException e) {
            System.err.println("Error loading ProductCard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRealtimeListener() {
        if (realtimeListener != null) return;
        realtimeListener = updatedSession -> {
            // Push mang theo endTime/currentPrice mới; cập nhật card đang hiển thị ngay để countdown không bị lệch.
            AuctionCache.addOrReplace(updatedSession);
            ProductCardController visibleCard = cardControllers.get(updatedSession.getId());
            if (visibleCard != null) {
                visibleCard.setAuctionSession(updatedSession);
            }
            // Fetch lại active từ server ở nền để đồng bộ thứ tự/section sau realtime push.
            loadActiveAuctionsAsync();
        };
        RealtimeAuctionBus.addAuctionListener(realtimeListener);
    }

    private void startRefreshTimer() {
        if (refreshTimer != null) return;
        // Poll nhẹ 10 giây/lần để giảm tải DB cloud; realtime push vẫn cập nhật ngay khi có auction mới.
        refreshTimer = new Timeline(new KeyFrame(Duration.seconds(10), event -> loadActiveAuctionsAsync()));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();
    }

    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
        for (ProductCardController controller : cardControllers.values()) {
            controller.cleanup();
        }
        cardControllers.clear();

        if (realtimeListener != null) {
            RealtimeAuctionBus.removeAuctionListener(realtimeListener);
            realtimeListener = null;
        }
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToSellerRole();
        cleanup();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        cleanup();
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
    }

    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        cleanup();
        SceneNavigator.loadScene(SceneNavigator.ITEMS, "items");
    }

    @FXML
    private void switchWalletPane(MouseEvent event) {
        cleanup();
        SceneNavigator.loadScene(SceneNavigator.WALLET1, "wallet");
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) {
        cleanup();
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }
}
