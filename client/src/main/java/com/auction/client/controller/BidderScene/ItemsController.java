package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ItemsController implements Initializable {
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private final List<ProductCardController> cardControllers = new ArrayList<>();

    @FXML private Label lblUsername;

    // Các container chứa thẻ sản phẩm cho từng danh mục
    @FXML private FlowPane containerArt;
    @FXML private FlowPane containerElec;
    @FXML private FlowPane containerVehicle;
    @FXML private FlowPane containerOther;

    @FXML private Button btnArtPane, btnElecPane, btnVehiclePane, btnOtherPane;
    @FXML private AnchorPane ArtPane, ElecPane, VehiclePane, OtherPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionContext.getCurrentUser() != null) {
            lblUsername.setText(SessionContext.getCurrentUser().getUsername());
        } else {
            lblUsername.setText("Guest User");
        }

        loadAuctionsAsync();
    }

    private void loadAuctionsAsync() {
        if (AuctionCache.hasActiveData()) {
            renderAllCategories(AuctionCache.getActive());
        } else {
            showStatusMessage("Loading items...", "#757575");
        }

        // Items chỉ hiển thị phiên đang bid được, nên bắt buộc dùng active cache.
        // Không update all cache ở đây để tránh làm mất lịch sử seller/admin.
        FxAsync.run("items-load", protocol::getActiveAuctions,
                this::renderAllCategories,
                error -> {
                    if (!AuctionCache.hasActiveData()) {
                        showStatusMessage("Could not connect to server.", "#c62828");
                    }
                });
    }

    private void renderAllCategories(List<AuctionSession> auctions) {
        // Dọn dẹp tất cả container trước khi nạp mới
        clearAllContainers();
        cleanupCards();

        List<AuctionSession> active = auctions == null ? List.of() : auctions.stream()
                .filter(this::isBiddableNow)
                .toList();
        if (active.isEmpty()) {
            showStatusMessage("No auctions available.", "#757575");
            return;
        }

        for (AuctionSession session : active) {
            if (session.getItem() == null || session.getItem().getItemType() == null) {
                loadProductCard(session, containerOther);
                continue;
            }
            String type = session.getItem().getItemType().toString().toUpperCase();

            // Phân loại sản phẩm vào đúng FlowPane dựa trên ItemType
            if (type.contains("ART")) {
                loadProductCard(session, containerArt);
            } else if (type.contains("ELEC")) {
                loadProductCard(session, containerElec);
            } else if (type.contains("VEHICLE")) {
                loadProductCard(session, containerVehicle);
            } else {
                loadProductCard(session, containerOther);
            }
        }
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

    private void loadProductCard(AuctionSession session, FlowPane container) {
        if (container == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
            Node card = loader.load();

            ProductCardController controller = loader.getController();
            controller.setAuctionSession(session);
            cardControllers.add(controller);

            container.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("Error loading ProductCard: " + e.getMessage());
        }
    }

    private void clearAllContainers() {
        if (containerArt != null) containerArt.getChildren().clear();
        if (containerElec != null) containerElec.getChildren().clear();
        if (containerVehicle != null) containerVehicle.getChildren().clear();
        if (containerOther != null) containerOther.getChildren().clear();
    }

    private void showStatusMessage(String message, String color) {
        // Hiển thị tin nhắn ở container mặc định (Art)
        if (containerArt == null) return;
        containerArt.getChildren().clear();
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: " + color + ";");
        containerArt.getChildren().add(label);
    }

    @FXML
    public void switchTab(ActionEvent event) {
        Object source = event.getSource();
        resetTabStyles();

        if (source == btnArtPane) {
            ArtPane.toFront();
            setActiveTabStyle(btnArtPane);
        } else if (source == btnElecPane) {
            ElecPane.toFront();
            setActiveTabStyle(btnElecPane);
        } else if (source == btnVehiclePane) {
            VehiclePane.toFront();
            setActiveTabStyle(btnVehiclePane);
        } else if (source == btnOtherPane) {
            OtherPane.toFront();
            setActiveTabStyle(btnOtherPane);
        }
    }

    private void resetTabStyles() {
        String defaultStyle = "-fx-background-color: white; -fx-text-fill: black;";
        btnArtPane.setStyle(defaultStyle);
        btnElecPane.setStyle(defaultStyle);
        btnVehiclePane.setStyle(defaultStyle);
        btnOtherPane.setStyle(defaultStyle);
    }

    private void setActiveTabStyle(Button btn) {
        btn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50;");
    }

    private void cleanupCards() {
        for (ProductCardController controller : cardControllers) {
            controller.cleanup();
        }
        cardControllers.clear();
    }

    @FXML public void switchHomePane(MouseEvent event) { cleanupCards(); SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "Home"); }
    @FXML public void switchMyBids(MouseEvent event) { cleanupCards(); SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "My Bids"); }
    @FXML public void switchWalletPane(MouseEvent event) { cleanupCards(); SceneNavigator.loadScene(SceneNavigator.WALLET1, "Wallet"); }
    @FXML public void switchSettingsPane(MouseEvent event) { cleanupCards(); SceneNavigator.loadScene(SceneNavigator.SETTING1, "Settings"); }
    @FXML public void switchSellerDB(MouseEvent event) { cleanupCards(); UserRoleSwitcher.switchToSellerRole(); SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "Seller Dashboard"); }
}