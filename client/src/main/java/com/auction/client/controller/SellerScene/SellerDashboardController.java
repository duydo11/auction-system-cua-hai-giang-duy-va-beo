package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * SellerDashboard — hiển thị thông tin seller, tạo phiên đấu giá mới.
 */
public class SellerDashboardController implements Initializable {

    @FXML private Label lblUsername;
    @FXML private TextField txtItemName;
    @FXML private TextField txtItemDesc;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtDurationHours;
    @FXML private Label lblCreateAuctionMsg;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
    }

    /**
     * Tạo phiên đấu giá mới — gửi data thật qua protocol.
     */
    @FXML
    private void handleCreateAuction(ActionEvent event) {
        if (lblCreateAuctionMsg == null) return;
        lblCreateAuctionMsg.setText("");
        lblCreateAuctionMsg.setStyle("-fx-text-fill: #c62828;");

        User u = SessionContext.getCurrentUser();
        if (!(u instanceof Seller seller)) {
            lblCreateAuctionMsg.setText("Chỉ tài khoản Seller có thể đăng phiên.");
            return;
        }

        String name = txtItemName != null ? txtItemName.getText().trim() : "";
        String desc = txtItemDesc != null ? txtItemDesc.getText().trim() : "";
        String priceRaw = txtStartPrice != null ? txtStartPrice.getText().trim().replace(",", "") : "";
        String hoursRaw = txtDurationHours != null ? txtDurationHours.getText().trim() : "";

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
            lblCreateAuctionMsg.setText("Đã tạo phiên thành công!");
            lblCreateAuctionMsg.setStyle("-fx-text-fill: #2e7d32;");
            if (txtItemName != null) txtItemName.clear();
            if (txtItemDesc != null) txtItemDesc.clear();
            if (txtStartPrice != null) txtStartPrice.clear();
            if (txtDurationHours != null) txtDurationHours.clear();
        } else {
            lblCreateAuctionMsg.setText(err);
        }
    }

    // ==================== Navigation ====================

    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }

    @FXML
    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }

    @FXML
    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }

    @FXML
    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }

    @FXML
    public void switchMylisting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "shipping home");
    }
}
