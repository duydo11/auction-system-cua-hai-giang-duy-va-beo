package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
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
 * Seller dashboard that shows the current seller's listed auctions.
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
        loadSellerAuctions();
    }

    private void loadSellerAuctions() {
        if (containerTopPicks != null) containerTopPicks.getChildren().clear();
        if (containerEndingSoon != null) containerEndingSoon.getChildren().clear();

        User current = SessionContext.getCurrentUser();
        if (current == null) {
            showMessage("Bạn chưa đăng nhập.");
            return;
        }

        List<AuctionSession> auctions = protocol.getActiveAuctions().stream()
                .filter(s -> s.getSeller() != null && s.getSeller().getId() == current.getId())
                .toList();

        if (auctions.isEmpty()) {
            showMessage("Bạn chưa có sản phẩm nào đang lên sàn.");
            return;
        }

        for (int i = 0; i < auctions.size(); i++) {
            HBox target = i % 2 == 0 ? containerTopPicks : containerEndingSoon;
            if (target != null) {
                loadProductCard(auctions.get(i), target);
            }
        }
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
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "shipping home");
    }
}
