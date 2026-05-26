package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class MyBidsController implements Initializable{
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    public void initialize(URL location, ResourceBundle resources) {
        loadBidHistoryOrFallback();
    }

    //Đổi home
    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "Home");
    }

    //Đổi seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) throws IOException {
        UserRoleSwitcher.switchToSellerRole();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    //Đổi items
    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ITEMS, "items");
    }

    //Đổi wallet
    @FXML
    private void switchWalletPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.WALLET1, "wallet");
    }

    //Đổi Settings
    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }


    @FXML
        private Button btnTotalPane;
        @FXML
        private Button btnActivePane;

        @FXML
        private AnchorPane TotalPane;
        @FXML
        private AnchorPane ActivePane;

    @FXML
    public void switchTab(ActionEvent event) throws IOException {
        if (event.getSource() == btnTotalPane) {
            TotalPane.toFront(); //Hiện Total
            btnTotalPane.setStyle("-fx-background-color: #3a3386; -fx-text-fill: white; -fx-border-color: white");
            btnActivePane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color: #3a3386");
        } else if (event.getSource() == btnActivePane) {
            ActivePane.toFront(); //Hiện Active
            btnActivePane.setStyle("-fx-background-color: #3a3386; -fx-text-fill: white; -fx-border-color: white");
            btnTotalPane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color: #3a3386");
        }
    }

    //HistoryCard
    @FXML
    private VBox containerTotal;

    private void loadBidHistoryOrFallback() {
        containerTotal.getChildren().clear();
        if (SessionContext.getCurrentUser() == null) {
            testLoadCards();
            return;
        }
        int myId = SessionContext.getCurrentUser().getId();
        for (AuctionSession session : protocol.getActiveAuctions()) {
            List<Bid> bids = protocol.getBidHistory(session.getId());
            for (Bid bid : bids) {
                if (bid.getBidder() != null && bid.getBidder().getId() == myId) {
                    Label row = new Label("Phiên #" + session.getId() + " · " + bid.getAmount());
                    row.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14;");
                    containerTotal.getChildren().add(row);
                }
            }
        }
        if (containerTotal.getChildren().isEmpty()) {
            testLoadCards();
        }
    }

    private void testLoadCards() {
        try {
            for (int i = 0; i < 4; i++) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/HistoryCard.fxml"));
                Node card = loader.load();
                card.getStyleClass().add("product-card");
                containerTotal.getChildren().add(card);
            }
        } catch (IOException e) {
            System.out.println("Lỗi rồi: Không tìm thấy file");
            e.printStackTrace();
        }
    }



}
