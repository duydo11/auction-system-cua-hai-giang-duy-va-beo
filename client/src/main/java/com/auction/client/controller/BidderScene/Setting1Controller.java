package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.network.NetworkCleanup;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class Setting1Controller implements Initializable {

    //Đổi home
    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "home");
    }

    //Đổi seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) throws IOException {
        UserRoleSwitcher.switchToSellerRole();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    //Đổi mybids
    @FXML
    private void switchMybidsPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
    }

    //Đổi wallet
    @FXML
    private void switchWalletPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.WALLET1, "wallet");
    }

    //Đổi items
    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ITEMS, "items");
    }

    //Đổi login
    @FXML
    private void switchLogin(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "login");
    }

    //Đổi lblname
    @FXML
    private Label lblUsername;
    @FXML
    private Label lblUsername1;
    @FXML
    private Label lblEmail;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        if (u != null) {
            lblUsername.setText(u.getUsername());
            lblUsername1.setText(u.getUsername());
            lblEmail.setText(u.getEmail());
        } else {
            lblUsername.setText("Guest");
            lblUsername1.setText("Guest");
            lblEmail.setText("—");
        }
    }

}
