package com.auction.client.controller.BidderScene;

import com.auction.client.MockData.DataStore;
import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.ui.AuctionRowFactory;
import com.auction.client.util.SceneNavigator;
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
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class ItemsController implements Initializable {
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    //Hiển thị Username
    @FXML
    private Label lblUsername;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionContext.getCurrentUser() != null) {
            lblUsername.setText(SessionContext.getCurrentUser().getUsername());
        } else if (DataStore.currentUser != null) {
            lblUsername.setText(DataStore.currentUser.getUsername());
        } else {
            lblUsername.setText("Guest User");
        }
        reloadAuctionsOrFallback();
    }

    //Đổi seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    //Chuyển myBids
    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
    }

    //Đổi HomePane
    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "home");
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


    //Đổi tab
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

    //Chuyển tab
    @FXML
    public void switchTab(ActionEvent event) throws IOException {
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
        } else if  (event.getSource() == btnVehiclePane) {
            VehiclePane.toFront();
            btnVehiclePane.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50");
            btnArtPane.setStyle("-fx-background-color: white");
            btnElecPane.setStyle("-fx-background-color: white");
            btnOtherPane.setStyle("-fx-background-color: white");
        }  else if (event.getSource() == btnOtherPane) {
            OtherPane.toFront();
            btnOtherPane.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 50");
            btnArtPane.setStyle("-fx-background-color: white");
            btnElecPane.setStyle("-fx-background-color: white");
            btnVehiclePane.setStyle("-fx-background-color: white");
        }
    }


    //Productcard Art (unchecked)
    @FXML
    private FlowPane containerArt;

    private void reloadAuctionsOrFallback() {
        containerArt.getChildren().clear();
        List<AuctionSession> auctions = protocol.getActiveAuctions();
        if (auctions.isEmpty()) {
            testLoadCards();
            return;
        }
        Runnable refresh = this::reloadAuctionsOrFallback;
        for (AuctionSession session : auctions) {
            containerArt.getChildren().add(AuctionRowFactory.bidRow(session, protocol, refresh));
        }
    }

    private void testLoadCards() {
        try {
            for (int i = 0; i < 6; i++) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
                Node card = loader.load();
                containerArt.getChildren().add(card);
            }
        } catch (IOException e) {
            System.out.println("Lỗi rồi: Không tìm thấy file CardItems.fxml");
            e.printStackTrace();
        }
    }

}
