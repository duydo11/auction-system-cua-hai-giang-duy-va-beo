package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.fx.SceneRealtime;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.ui.AuctionRowFactory;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import javafx.event.ActionEvent;
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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MyBidsController implements Initializable {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private final Consumer<AuctionSession> realtimeListener = s -> {
        reloadActiveAuctions();
        reloadBidHistory();
    };

    @FXML
    private Button btnTotalPane;
    @FXML
    private Button btnActivePane;
    @FXML
    private AnchorPane TotalPane;
    @FXML
    private AnchorPane ActivePane;
    @FXML
    private VBox activeAuctionRows;
    @FXML
    private VBox bidHistoryRows;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SceneRealtime.attachAuctionUpdates(activeAuctionRows, realtimeListener);
        reloadActiveAuctions();
        reloadBidHistory();
    }

    private void reloadActiveAuctions() {
        activeAuctionRows.getChildren().clear();
        Runnable refresh = () -> {
            reloadActiveAuctions();
            reloadBidHistory();
        };
        for (AuctionSession session : protocol.getActiveAuctions()) {
            activeAuctionRows.getChildren().add(AuctionRowFactory.bidRow(session, protocol, refresh));
        }
    }

    private void reloadBidHistory() {
        bidHistoryRows.getChildren().clear();
        User me = SessionContext.getCurrentUser();
        if (me == null) {
            bidHistoryRows.getChildren().add(new Label("Đăng nhập để xem lịch sử."));
            return;
        }
        int myId = me.getId();
        for (AuctionSession session : protocol.getActiveAuctions()) {
            List<Bid> bids = protocol.getBidHistory(session.getId());
            for (Bid b : bids) {
                if (b.getBidder() != null && b.getBidder().getId() == myId) {
                    String line = String.format(
                            "Phiên #%d · %.2f · %s",
                            session.getId(),
                            b.getAmount(),
                            b.getTime() != null ? b.getTime().toString() : ""
                    );
                    bidHistoryRows.getChildren().add(new Label(line));
                }
            }
        }
        if (bidHistoryRows.getChildren().isEmpty()) {
            bidHistoryRows.getChildren().add(new Label("Chưa có bid trong các phiên đang mở."));
        }
    }

    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/BidderDashboard.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) throws IOException {
        Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/SellerScene/SellerDashboard.fxml")));
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(sellerView);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/Items.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy Items.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    private void switchWalletPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Wallet1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Setting1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
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

    //HistoryCard
    @FXML
    private VBox containerTotal;
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
