package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.fx.SceneRealtime;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.ui.AuctionRowFactory;
import com.auction.shared.model.auction.AuctionSession;
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

public class ItemsController implements Initializable {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private final Consumer<AuctionSession> realtimeListener = s -> reloadAuctionLists();

    @FXML
    private Label lblUsername;
    @FXML
    private VBox auctionRowsArt;
    @FXML
    private VBox auctionRowsElec;
    @FXML
    private VBox auctionRowsVehicle;
    @FXML
    private VBox auctionRowsOther;

    @FXML
    private Button btnArtPane;
    @FXML
    private Button btnElecPane;
    @FXML
    private Button btnVehiclePane;
    @FXML
    private Button btnOtherPane;
    @FXML
    private AnchorPane ArtPane;
    @FXML
    private AnchorPane ElecPane;
    @FXML
    private AnchorPane VehiclePane;
    @FXML
    private AnchorPane OtherPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
        SceneRealtime.attachAuctionUpdates(lblUsername, realtimeListener);
        reloadAuctionLists();
    }

    private void reloadAuctionLists() {
        clearRows();
        List<AuctionSession> auctions = protocol.getActiveAuctions();
        Runnable refresh = this::reloadAuctionLists;
        for (AuctionSession session : auctions) {
            String type = session.getItem() != null ? session.getItem().getItemType() : "OTHER";
            VBox target = switch (type) {
                case "ART" -> auctionRowsArt;
                case "ELECTRONICS" -> auctionRowsElec;
                case "VEHICLE" -> auctionRowsVehicle;
                default -> auctionRowsOther;
            };
            target.getChildren().add(AuctionRowFactory.bidRow(session, protocol, refresh));
        }
    }

    private void clearRows() {
        auctionRowsArt.getChildren().clear();
        auctionRowsElec.getChildren().clear();
        auctionRowsVehicle.getChildren().clear();
        auctionRowsOther.getChildren().clear();
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        try {
            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/SellerScene/SellerDashboard.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(sellerView);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy SellerDashboard.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        try {
            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/MyBids.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(sellerView);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy MyBids.fxml");
            e.printStackTrace();
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
    private void switchSettingsPane1(MouseEvent event) throws IOException {
        switchSettingsPane(event);
    }

    @FXML
    public void switchTab(ActionEvent event) {
        if (event.getSource() == btnArtPane) {
            ArtPane.toFront();
            btnArtPane.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50");
            btnElecPane.setStyle("-fx-background-color: white");
            btnVehiclePane.setStyle("-fx-background-color: white");
            btnOtherPane.setStyle("-fx-background-color: white");
        } else if (event.getSource() == btnElecPane) {
            ElecPane.toFront();
            btnElecPane.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50");
            btnArtPane.setStyle("-fx-background-color: white");
            btnVehiclePane.setStyle("-fx-background-color: white");
            btnOtherPane.setStyle("-fx-background-color: white");
        } else if (event.getSource() == btnVehiclePane) {
            VehiclePane.toFront();
            btnVehiclePane.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50");
            btnArtPane.setStyle("-fx-background-color: white");
            btnElecPane.setStyle("-fx-background-color: white");
            btnOtherPane.setStyle("-fx-background-color: white");
        } else if (event.getSource() == btnOtherPane) {
            OtherPane.toFront();
            btnOtherPane.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50");
            btnArtPane.setStyle("-fx-background-color: white");
            btnElecPane.setStyle("-fx-background-color: white");
            btnVehiclePane.setStyle("-fx-background-color: white");
        }
    }
}
