package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
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
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/BidderDashboard.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    //Đổi seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) throws IOException {
        // 1. Load file giao diện Seller
        Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/SellerScene/SellerDashboard.fxml")));
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(sellerView);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    //Đổi items
    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        try {
            // 1. Load file giao diện Items
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/Items.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

    //Đổi wallet
    @FXML
    private void switchWalletPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Wallet1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    //Đổi Settings
    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Setting1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
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
