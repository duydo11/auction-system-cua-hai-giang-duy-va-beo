package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.network.NetworkCleanup;
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
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class Setting1Controller implements Initializable {

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

    //Đổi mybids
    @FXML
    private void switchMybidsPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/MyBids.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
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

    //Đổi login
    @FXML
    private void switchLogin(MouseEvent event) throws IOException {
        NetworkCleanup.logoutClient();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
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
