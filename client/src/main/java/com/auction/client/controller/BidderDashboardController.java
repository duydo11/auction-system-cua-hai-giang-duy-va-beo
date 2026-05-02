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
        if (DataStore.currentUser != null) {
            lblUsername.setText(DataStore.currentUser.getUsername());
        } else {
            lblUsername.setText("Guest User");
        }

        testLoadCards(); //Test productcard
    }

    //Đổi giao diện Seller
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

    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
    try {
        // 1. Load file giao diện MyBids
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/MyBids.fxml")));
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
