package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Màn hình My Listing cho seller.
 *
 * <p>Phần truy vấn danh sách sản phẩm được chạy ở task nền để khi mở màn này
 * cửa sổ JavaFX không bị đứng do chờ backend.</p>
 */
public class MyListingController {

    @FXML private HBox overlayPane;
    @FXML private VBox containerList;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    public void initialize() {
        loadSellerListingsAsync();
    }

    private void loadSellerListingsAsync() {
        var current = SessionContext.getCurrentUser();
        if (current == null) {
            containerList.getChildren().clear();
            showMessage("You are not logged in.");
            return;
        }

        if (AuctionCache.hasData()) {
            renderSellerListings(filterSellerAuctions(AuctionCache.get(), current.getId()));
        } else {
            containerList.getChildren().clear();
            showMessage("Loading your listings...");
        }

        if (AuctionCache.isStale()) {
            // Chuyển request này ra thread nền để tránh lỗi "Not Responding" khi chờ dữ liệu.
            FxAsync.run("my-listings-load", protocol::getActiveAuctions,
                    auctions -> {
                        AuctionCache.update(auctions);
                        renderSellerListings(filterSellerAuctions(auctions, current.getId()));
                    },
                    error -> {
                        if (!AuctionCache.hasData()) {
                            showMessage("Could not load product cards.");
                        }
                    });
        }
    }

    private void renderSellerListings(List<AuctionSession> auctions) {
        containerList.getChildren().clear();
        if (auctions.isEmpty()) {
            showMessage("No products have been listed yet.");
            return;
        }

        for (AuctionSession session : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
                Node card = loader.load();
                ProductCardController controller = loader.getController();
                controller.setAuctionSession(session);
                containerList.getChildren().add(card);
            } catch (IOException e) {
                showMessage("Could not load product cards.");
                break;
            }
        }
    }

    private List<AuctionSession> filterSellerAuctions(List<AuctionSession> auctions, int sellerId) {
        return auctions.stream()
                .filter(s -> s.getSeller() != null && s.getSeller().getId() == sellerId)
                .toList();
    }

    private void showMessage(String message) {
        containerList.getChildren().clear();
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: #666666;");
        containerList.getChildren().add(label);
    }

    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToBidderRole();
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }

    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }

    public void switchHomePane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller home");
    }

    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }

    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }

    public void handleAddproduct(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/AddProductDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create new listing");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            overlayPane.setVisible(true);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            overlayPane.setVisible(false);
            loadSellerListingsAsync();
        } catch (IOException e) {
            overlayPane.setVisible(false);
            e.printStackTrace();
        }
    }
}
