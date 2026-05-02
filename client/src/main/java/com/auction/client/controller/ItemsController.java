package com.auction.client.controller;

import com.auction.client.MockData.DataStore;
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
import java.util.Objects;
import java.util.ResourceBundle;

public class ItemsController implements Initializable {
    //Hiển thị Username
    @FXML
    private Label lblUsername;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (DataStore.currentUser != null) {
            // Hiển thị Username đã lưu trong đối tượng currentUser
            lblUsername.setText(DataStore.currentUser.getUsername());
        } else {
            lblUsername.setText("Guest User");
        }
        testLoadCards();
    }

    //Đổi seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        try {
            // 1. Load file giao diện Seller
            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/SellerScene/SellerDashboard.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(sellerView);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file /fxml/SellerDashboard.fxml");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

    //Chuyển myBids
    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        try {
            // 1. Load file giao diện MyBids
            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/MyBids.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(sellerView);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file /fxml/SellerDashboard.fxml");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

    //Đổi HomePane
    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/BidderDashboard.fxml"));
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

    //Đổi Settings
    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Setting1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    //Đổi Settings
    @FXML
    private void switchSettingsPane1(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Setting1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
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


    //Productcard Art
    @FXML
    private FlowPane containerArt;

    private void testLoadCards() {
        try {
            for (int i = 0; i < 10; i++) {
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
