package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.HistoryCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class MyBidsController implements Initializable {
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML private Button btnTotalPane;
    @FXML private Button btnActivePane;
    @FXML private AnchorPane TotalPane;
    @FXML private AnchorPane ActivePane;
    @FXML private VBox containerTotal;
    @FXML private VBox containerActive;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadBidHistory();
        loadActiveBidAuctions();
        if (TotalPane != null) {
            TotalPane.toFront();
        }
    }

    @FXML
    private void switchHomePane(MouseEvent event) {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "Home");
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToSellerRole();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ITEMS, "items");
    }

    @FXML
    private void switchWalletPane(MouseEvent event) {
        SceneNavigator.loadScene(SceneNavigator.WALLET1, "wallet");
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) {
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }

    @FXML
    public void switchTab(ActionEvent event) {
        if (event.getSource() == btnTotalPane) {
            TotalPane.toFront();
            btnTotalPane.setStyle("-fx-background-color: #3a3386; -fx-text-fill: white; -fx-border-color: white");
            btnActivePane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color: #3a3386");
        } else if (event.getSource() == btnActivePane) {
            ActivePane.toFront();
            btnActivePane.setStyle("-fx-background-color: #3a3386; -fx-text-fill: white; -fx-border-color: white");
            btnTotalPane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color: #3a3386");
        }
    }

    private void loadBidHistory() {
        containerTotal.getChildren().clear();
        if (SessionContext.getCurrentUser() == null) {
            showEmptyState(containerTotal, "Bạn chưa đăng nhập.");
            return;
        }

        int myId = SessionContext.getCurrentUser().getId();
        List<Bid> myBids = new ArrayList<>();
        for (AuctionSession session : protocol.getActiveAuctions()) {
            for (Bid bid : protocol.getBidHistory(session.getId())) {
                if (bid.getBidder() != null && bid.getBidder().getId() == myId) {
                    myBids.add(bid);
                }
            }
        }

        myBids.sort(Comparator.comparing(Bid::getTime).reversed());
        if (myBids.isEmpty()) {
            showEmptyState(containerTotal, "Chưa có lịch sử bid nào.");
            return;
        }

        for (Bid bid : myBids) {
            loadHistoryCard(bid);
        }
    }

    private void loadActiveBidAuctions() {
        containerActive.getChildren().clear();
        if (SessionContext.getCurrentUser() == null) {
            showEmptyState(containerActive, "Bạn chưa đăng nhập.");
            return;
        }

        int myId = SessionContext.getCurrentUser().getId();
        boolean found = false;
        for (AuctionSession session : protocol.getActiveAuctions()) {
            boolean hasMyBid = protocol.getBidHistory(session.getId()).stream()
                    .anyMatch(b -> b.getBidder() != null && b.getBidder().getId() == myId);
            if (hasMyBid) {
                found = true;
                containerActive.getChildren().add(new Label(
                        "Phiên #" + session.getId() + " · " +
                        (session.getItem() != null ? session.getItem().getName() : "Unknown item") +
                        " · Giá hiện tại: " + String.format("%.0f VND", session.getCurrentPrice())
                ));
            }
        }
        if (!found) {
            showEmptyState(containerActive, "Không có phiên đang hoạt động mà bạn đã bid.");
        }
    }

    private void loadHistoryCard(Bid bid) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/HistoryCard.fxml"));
            Node card = loader.load();
            HistoryCardController controller = loader.getController();
            controller.setBid(bid);
            containerTotal.getChildren().add(card);
        } catch (IOException e) {
            Label fallback = new Label("Phiên #" + bid.getAuctionSession().getId() + " · " + bid.getAmount());
            containerTotal.getChildren().add(fallback);
        }
    }

    private void showEmptyState(VBox container, String message) {
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: #666666;");
        container.getChildren().add(label);
    }
}
