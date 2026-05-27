package com.auction.client.controller.BidderScene;

import com.auction.client.MockData.DataStore;
import com.auction.client.SessionContext;
import com.auction.client.controller.Card.ProductCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.ui.AuctionRowFactory;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
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
import javafx.scene.layout.Pane;
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
        UserRoleSwitcher.switchToSellerRole();
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

    @FXML
    private FlowPane containerElec;

    @FXML
    private FlowPane containerOther;

    @FXML
    private FlowPane containerVehicle;

    private void reloadAuctionsOrFallback() {
        if (containerArt != null) containerArt.getChildren().clear();
        if (containerVehicle != null) containerVehicle.getChildren().clear();
        if (containerElec != null) containerElec.getChildren().clear();
        if (containerOther != null) containerOther.getChildren().clear();

        // 2. Lấy dữ liệu từ server
        List<AuctionSession> auctions = protocol.getActiveAuctions();

        // 3. Phân loại và nạp card vào đúng container
        for (AuctionSession session : auctions) {
            Item item = session.getItem();
            Pane targetContainer = null;

            // Phân loại dựa trên class thực tế của Item (Dùng mẫu Model bạn đã có)
            if (item instanceof Art) {
                targetContainer = containerArt;
            } else if (item instanceof Vehicle) {
                targetContainer = containerVehicle;
            } else if (item instanceof Electronics) {
                targetContainer = containerElec;
            }

            // Nạp Card vào
            if (targetContainer != null) {
                loadProductCard(session, targetContainer);
            }
        }
    }
    private void loadProductCard(AuctionSession session, Pane targetContainer) {
        try {
            // Load file FXML của mẫu Card
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
            Node card = loader.load();

            // Lấy controller của Card để truyền dữ liệu vào
            ProductCardController controller = loader.getController();
            controller.setAuctionSession(session); // Đảm bảo ProductCardController có hàm setData này

            // Thêm card vào UI
            targetContainer.getChildren().add(card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
