package com.auction.client.controller.BidderScene;

import com.auction.client.MockData.DataStore;
import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.ui.AuctionRowFactory;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller cho màn Items (danh sách sản phẩm đấu giá).
 */
public class ItemsController implements Initializable {
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML private Label lblUsername;
    @FXML private FlowPane containerArt;
    @FXML private Button btnArtPane;
    @FXML private Button btnElecPane;
    @FXML private Button btnVehiclePane;
    @FXML private Button btnOtherPane;
    @FXML private AnchorPane ArtPane;
    @FXML private AnchorPane ElecPane;
    @FXML private AnchorPane VehiclePane;
    @FXML private AnchorPane OtherPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (SessionContext.getCurrentUser() != null) {
            lblUsername.setText(SessionContext.getCurrentUser().getUsername());
        } else if (DataStore.currentUser != null) {
            lblUsername.setText(DataStore.currentUser.getUsername());
        } else {
            lblUsername.setText("Guest User");
        }

        loadAuctionsAsync();
    }

    private void loadAuctionsAsync() {
        if (AuctionCache.hasData()) {
            renderAuctionRows(AuctionCache.get());
        } else {
            containerArt.getChildren().clear();
            showLoadingMessage("Loading items...");
        }

        if (AuctionCache.isStale()) {
            FxAsync.run("items-load", protocol::getActiveAuctions,
                    auctions -> {
                        AuctionCache.update(auctions);
                        renderAuctionRows(auctions);
                    },
                    error -> {
                        if (!AuctionCache.hasData()) {
                            showLoadingMessage("Could not load items. Please try again.");
                        }
                    });
        }
    }

    private void renderAuctionRows(List<AuctionSession> auctions) {
        containerArt.getChildren().clear();

        if (auctions.isEmpty()) {
            showLoadingMessage("No items available at the moment.");
            return;
        }

        Runnable refresh = this::loadAuctionsAsync;
        for (AuctionSession session : auctions) {
            containerArt.getChildren().add(
                    AuctionRowFactory.bidRow(session, protocol, refresh)
            );
        }
    }

    private void showLoadingMessage(String message) {
        containerArt.getChildren().clear();
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: #666666;");
        containerArt.getChildren().add(label);
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToSellerRole();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
    }

    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "home");
    }

    @FXML
    private void switchWalletPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.WALLET1, "wallet");
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }

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
}
