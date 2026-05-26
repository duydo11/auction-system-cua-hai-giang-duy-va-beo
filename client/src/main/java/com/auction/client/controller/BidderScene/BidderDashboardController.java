package com.auction.client.controller.BidderScene;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Bidder dashboard controller.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>load active auctions from the backend instead of static placeholder cards,</li>
 *   <li>create one {@code ProductCard.fxml} per {@link AuctionSession},</li>
 *   <li>store card controllers by session id so realtime pushes can update the right card,</li>
 *   <li>remove only this controller's listener during cleanup to avoid breaking other screens.</li>
 * </ul>
 */
public class BidderDashboardController implements Initializable {

    @FXML private Label lblUsername;
    @FXML private HBox containerTopPicks;
    @FXML private HBox containerEndingSoon;
    
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private final Map<Integer, ProductCardController> cardControllers = new HashMap<>();
    private Consumer<AuctionSession> realtimeListener;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Display username
        if (SessionContext.getCurrentUser() != null) {
            lblUsername.setText(SessionContext.getCurrentUser().getUsername());
        } else {
            lblUsername.setText("Guest User");
        }
        
        // Load real auctions
        loadActiveAuctions();
        
        // Listen for realtime updates
        setupRealtimeListener();
    }
    
    /**
     * Load active auctions từ backend.
     */
    private void loadActiveAuctions() {
        try {
            List<AuctionSession> auctions = protocol.getActiveAuctions();
            
            if (auctions.isEmpty()) {
                // Show empty state on the first available FXML container.
                showDashboardMessage("Chưa có phiên đấu giá nào", "#757575");
                return;
            }
            
            // Clear containers
            if (containerTopPicks != null) {
                containerTopPicks.getChildren().clear();
            }
            if (containerEndingSoon != null) {
                containerEndingSoon.getChildren().clear();
            }
            
            // Load cards
            for (int i = 0; i < auctions.size(); i++) {
                AuctionSession session = auctions.get(i);
                HBox targetContainer = i % 2 == 0 ? containerTopPicks : containerEndingSoon;
                if (targetContainer != null) {
                    loadProductCard(session, targetContainer);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading auctions: " + e.getMessage());
            e.printStackTrace();
            
            // Show error state without throwing another NPE if FXML ids change.
            showDashboardMessage("Không thể kết nối server", "#c62828");
        }
    }

    /**
     * Adds a status label to the first visible dashboard row.
     */
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
    
    /**
     * Load một ProductCard với data binding.
     */
    private void loadProductCard(AuctionSession session, HBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Card/ProductCard.fxml")
            );
            Node card = loader.load();
            
            // Get controller và bind data
            ProductCardController controller = loader.getController();
            controller.setAuctionSession(session);
            
            // Store controller để update sau
            cardControllers.put(session.getId(), controller);
            
            // Add to container
            card.getStyleClass().add("product-card");
            container.getChildren().add(card);
            
        } catch (IOException e) {
            System.err.println("Error loading ProductCard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Setup realtime listener cho price updates.
     */
    private void setupRealtimeListener() {
        if (realtimeListener != null) return;
        realtimeListener = updatedSession -> {
            // Update card nếu đang hiển thị
            ProductCardController controller = cardControllers.get(updatedSession.getId());
            if (controller != null) {
                Platform.runLater(() -> {
                    controller.updatePrice(updatedSession.getCurrentPrice());
                    // Re-bind để update status, time, etc.
                    controller.setAuctionSession(updatedSession);
                });
            }
        };
        RealtimeAuctionBus.addAuctionListener(realtimeListener);
    }
    
    /**
     * Cleanup khi rời scene.
     */
    public void cleanup() {
        // Stop all countdown timers
        for (ProductCardController controller : cardControllers.values()) {
            controller.cleanup();
        }
        cardControllers.clear();
        
        // Remove realtime listener
        if (realtimeListener != null) {
            RealtimeAuctionBus.removeAuctionListener(realtimeListener);
            realtimeListener = null;
        }
    }
    
    // ==================== Navigation ====================
    
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
