package com.auction.client.controller;

import com.auction.client.MockData.DataStore;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class SellerDashboardController implements Initializable {

    @FXML
    private Label lblUsername;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (DataStore.currentUser != null) {
            // Hiển thị Username đã lưu trong đối tượng currentUser
            lblUsername.setText(DataStore.currentUser.getUsername());
        } else {
            // Nếu chưa đăng nhập (chạy thẳng Dashboard) thì hiện Guest
            lblUsername.setText("Guest User");
        }
    }
    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        try {
            // 1. Load file giao diện Seller

            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderDashboard.fxml")));

            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();

            Scene scene = new Scene(sellerView);
            stage.setScene(scene);
            stage.centerOnScreen();

            stage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file /fxml/BidderDashboard.fxml");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

}
