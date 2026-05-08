package com.auction.client.controller.BidderScene;

import com.auction.client.MockData.DataStore;
import com.auction.client.SessionContext;
import com.auction.client.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class BidderDashboardController implements Initializable {

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

        testLoadCards(); //Test productcard
    }

    //Đổi giao diện Seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    //Đổi My bids
    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
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

    //Test Product Card
    @FXML
    private HBox container1;
    @FXML
    private HBox container2;
    private void testLoadCards() {
        try {
            for (int i = 0; i < 4; i++) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
                Node card = loader.load();
                card.getStyleClass().add("product-card");
                container1.getChildren().add(card);

                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/fxml/Card/ProductCard.fxml"));
                Node card2 = loader2.load();
                card.getStyleClass().add("product-card");
                container2.getChildren().add(card2);
            }
        } catch (IOException e) {
            System.out.println("Lỗi rồi: Không tìm thấy file CardItems.fxml");
            e.printStackTrace();
        }
    }

}
