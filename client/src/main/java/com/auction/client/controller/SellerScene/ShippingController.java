package com.auction.client.controller.SellerScene;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ShippingController {

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
    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }
    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }
    public void switchHomePane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller home");
    }
    public void switchMylisting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "shipping home");
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
            btnTotalPane.setStyle("-fx-background-color:  #438bcf; -fx-text-fill: white; -fx-border-color: white");
            btnActivePane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color:  #438bcf");
        } else if (event.getSource() == btnActivePane) {
            ActivePane.toFront(); //Hiện Active
            btnActivePane.setStyle("-fx-background-color:  #438bcf; -fx-text-fill: white; -fx-border-color: white");
            btnTotalPane.setStyle("-fx-background-color: white; -fx-text-fill:  #438bcf; -fx-border-color:  #438bcf");
        }
    }

}