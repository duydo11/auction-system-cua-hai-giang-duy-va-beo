package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.ResourceBundle;

public class SellerDashboardController implements Initializable {

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

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
    }

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
    }

    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        try {
            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/BidderDashboard.fxml")));

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
