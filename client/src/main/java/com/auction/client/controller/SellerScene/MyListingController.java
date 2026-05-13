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

public class MyListingController {

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
    /*
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
    }
    /*: CreateAuction - Data thật (Giang chưa check)
    @FXML
    private void handleCreateAuction(ActionEvent event) {
        lblCreateAuctionMsg.setText("");
        lblCreateAuctionMsg.setStyle("-fx-text-fill: #c62828;");

        User u = SessionContext.getCurrentUser();
        if (!(u instanceof Seller seller)) {
            lblCreateAuctionMsg.setText("Chỉ tài khoản Seller có thể đăng phiên.");
            return;
        }

        String name = txtItemName.getText().trim();
        String desc = txtItemDesc.getText().trim();
        String priceRaw = txtStartPrice.getText().trim().replace(",", "");
        String hoursRaw = txtDurationHours.getText().trim();

        if (name.isEmpty() || desc.isEmpty() || priceRaw.isEmpty() || hoursRaw.isEmpty()) {
            lblCreateAuctionMsg.setText("Điền đủ các ô.");
            return;
        }
        double startPrice;
        long hours;
        try {
            startPrice = Double.parseDouble(priceRaw);
            hours = Long.parseLong(hoursRaw);
        } catch (NumberFormatException e) {
            lblCreateAuctionMsg.setText("Giá hoặc số giờ không hợp lệ.");
            return;
        }
        if (startPrice <= 0 || hours <= 0) {
            lblCreateAuctionMsg.setText("Giá và thời gian phải > 0.");
            return;
        }

        Electronics item = new Electronics(0, name, desc, seller, 12);
        LocalDateTime start = LocalDateTime.now();
        AuctionSession session = new AuctionSession(0, seller, item, startPrice, start, start.plusHours(hours));

        String err = protocol.createAuctionOrError(session);
        if (err == null) {
            lblCreateAuctionMsg.setText("Đã tạo phiên #" + session.getId() + " — các bidder thấy ngay (push).");
            lblCreateAuctionMsg.setStyle("-fx-text-fill: #2e7d32;");
            txtItemName.clear();
            txtItemDesc.clear();
            txtStartPrice.clear();
            txtDurationHours.clear();
        } else {
            lblCreateAuctionMsg.setText(err);
        }
    } */

    //Đổi bidder
    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }
    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }
    public void switchHomePane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller home");
    }
    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }

    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }



    public void handleAddproduct(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/AddProductDialog.fxml"));
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
