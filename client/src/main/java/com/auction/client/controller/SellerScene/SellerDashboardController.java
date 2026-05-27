package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Dashboard cho seller, hiển thị các phiên đấu giá thuộc về người bán hiện tại.
 *
 * <p>Các request tới server/cơ sở dữ liệu được chạy ở task nền để
 * JavaFX Application Thread không bị block khi dữ liệu tải chậm.</p>
 */
public class SellerDashboardController implements Initializable {

    @FXML private Label lblUsername;
    @FXML private HBox containerTopPicks;
    @FXML private HBox containerEndingSoon;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
        loadSellerAuctionsAsync();
    }

    private void loadSellerAuctionsAsync() {
        clearContainers();
        User current = SessionContext.getCurrentUser();
        if (current == null) {
            showMessage("You are not logged in.");
            return;
        }

        if (AuctionCache.hasData()) {
            renderSellerAuctions(filterSellerAuctions(AuctionCache.get(), current));
        } else {
            showMessage("Loading your listings...");
        }

        if (AuctionCache.isStale()) {
            FxAsync.run("seller-dashboard-load", protocol::getAllAuctions,
                    auctions -> {
                        AuctionCache.update(auctions);
                        renderSellerAuctions(filterSellerAuctions(auctions, current));
                    },
                    error -> {
                        if (!AuctionCache.hasData()) {
                            showMessage("Could not load seller listings.");
                        }
                    });
        }
    }

    private void renderSellerAuctions(List<AuctionSession> auctions) {
        clearContainers();
        if (auctions.isEmpty()) {
            showMessage("Bạn chưa tạo sản phẩm nào.");
            return;
        }

        for (int i = 0; i < auctions.size(); i++) {
            HBox target = i % 2 == 0 ? containerTopPicks : containerEndingSoon;
            if (target != null) {
                loadProductCard(auctions.get(i), target);
            }
        }
    }

    private List<AuctionSession> filterSellerAuctions(List<AuctionSession> auctions, User seller) {
        return auctions.stream()
                .filter(s -> s.getSeller() != null && s.getSeller().getId() == seller.getId())
                .toList();
    }

    private void clearContainers() {
        if (containerTopPicks != null) containerTopPicks.getChildren().clear();
        if (containerEndingSoon != null) containerEndingSoon.getChildren().clear();
    }

    private void loadProductCard(AuctionSession session, HBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
            Node card = loader.load();
            ProductCardController controller = loader.getController();
            controller.setAuctionSession(session);
            container.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("Cannot load seller product card: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        HBox target = containerTopPicks != null ? containerTopPicks : containerEndingSoon;
        if (target != null) {
            target.getChildren().clear();
            Label label = new Label(message);
            label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: #666666;");
            target.getChildren().add(label);
        }
    }

    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToBidderRole();
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }

    @FXML
    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }

    @FXML
    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }

    @FXML
    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }

    @FXML
    public void switchMylisting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "my listings");
    }
}
