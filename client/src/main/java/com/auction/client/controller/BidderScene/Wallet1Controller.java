package com.auction.client.controller.BidderScene;

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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class Wallet1Controller implements Initializable {
    @FXML
    private HBox overlayPane;

    //Đổi home
    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "home");
    }

    //Đổi seller
    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
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

    //Đổi mybids
    @FXML
    public void switchMybidsPane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
    }

    //Đổi settings
    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }

    //Hiển thị Username
    @FXML
    private Label lblUsername;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
    }

    @FXML //Test - Không phải data thật
    private VBox containerTrans;
    private void testLoadCards() {
        try {
            for (int i = 0; i < 10; i++) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/TransHisCard.fxml"));
                Node card = loader.load();
                containerTrans.getChildren().add(card);
            }
        } catch (IOException e) {
            System.out.println("Lỗi rồi: Không tìm thấy file CardItems.fxml");
            e.printStackTrace();
        }
    }
    public void handleDeposit(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/DepositAction.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Đăng sản phẩm mới");

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            overlayPane.setVisible(true);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            overlayPane.setVisible(false);// Dừng mọi thứ ở trang chính cho đến khi Dialog này đóng

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void handleWithdraw(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/WithdrawAction.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Đăng sản phẩm mới");

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            overlayPane.setVisible(true);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            overlayPane.setVisible(false);// Dừng mọi thứ ở trang chính cho đến khi Dialog này đóng

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}