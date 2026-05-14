package com.auction.client.controller.SellerScene;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class Setting2Controller {

    @FXML
    private Label lblUsername;
    @FXML
    private TextField txtItemName;
    @FXML
    private TextField txtItemDesc;
    @FXML
    private TextField txtStartPrice;
    @FXML
    private TextField txtDurationHours;
    @FXML
    private Label lblCreateAuctionMsg;
    @FXML
    private HBox overlayPane;


    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    //Đổi bidder
    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }
    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }
    public void switchMylisting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "shipping home");
    }
    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }
    public void switchHomePane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller home");
    }
    public void switchLogin(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "login home");
    }


}