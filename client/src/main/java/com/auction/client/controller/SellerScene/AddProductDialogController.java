package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AddProductDialogController implements Initializable {
    @FXML private TextArea txtProductName, txtStartingPrice;
    @FXML private TextArea txtProductDetails;
    @FXML private ComboBox<String> cbCategory;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private ComboBox<String> cbStartHour, cbStartMin, cbStartAMPM;
    @FXML private ComboBox<String> cbEndHour, cbEndMin, cbEndAMPM;
    @FXML private Label lblMessage;
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    public void initialize(URL location, ResourceBundle resources) {
        cbCategory.getItems().addAll(
                "Arts",
                "Vehicles",
                "Electronics",
                "Others"
        );
        // Đổ dữ liệu cho giờ (1-12)
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 1; i <= 12; i++) hours.add(String.format("%02d", i));
        cbStartHour.setItems(hours);
        cbEndHour.setItems(hours);
        // Đổ dữ liệu cho phút (00-59)
        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) minutes.add(String.format("%02d", i));
        cbStartMin.setItems(minutes);
        cbEndMin.setItems(minutes);
        // Đổ dữ liệu AM/PM
        ObservableList<String> ampm = FXCollections.observableArrayList("AM", "PM");
        cbStartAMPM.setItems(ampm);
        cbEndAMPM.setItems(ampm);
    }
    private LocalDateTime combineDateTime(DatePicker datePicker, ComboBox<String> hourCB, ComboBox<String> minCB, ComboBox<String> ampmCB) {
        LocalDate date = datePicker.getValue();
        int hour = Integer.parseInt(hourCB.getValue());
        int minute = Integer.parseInt(minCB.getValue());
        String ampm = ampmCB.getValue();
        // Chuyển đổi sang hệ 24h
        if (ampm.equals("PM") && hour < 12) hour += 12;
        if (ampm.equals("AM") && hour == 12) hour = 0;

        return date.atTime(hour, minute);
    }
    @FXML
    private void handleCreateAuction(MouseEvent mouseEvent) {
        try {
            // 1. Lấy thông tin cơ bản
            String name = txtProductName.getText().trim();
            String desc = txtProductDetails.getText().trim();
            String priceStr = txtStartingPrice.getText().trim().replace(",", "");

            // 2. Lấy thời gian (Sử dụng hàm ở Bước 4)
            LocalDateTime start = combineDateTime(dpStartDate, cbStartHour, cbStartMin, cbStartAMPM);
            LocalDateTime end = combineDateTime(dpEndDate, cbEndHour, cbEndMin, cbEndAMPM);

            if (end.isBefore(start)) {
                lblMessage.setText("End date must be after start date!");
                return;
            }

            User u = SessionContext.getCurrentUser();
            if (!(u instanceof Seller seller)) {
                lblMessage.setText("You are not seller!");
                return;
            }
            if (name.isEmpty() || priceStr.isEmpty() || start == null || end == null) {
                lblMessage.setText("Please fill in all information");
                return;
            }
            //Kiểm tra giá tiền
            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                lblMessage.setText("Invalid price format");
                return;
            }

            Electronics item = new Electronics(0, name, desc, seller, 12);
            AuctionSession session = new AuctionSession(0, seller, item, price, start, end);

            // 4. Gửi qua mạng bằng protocol của Hoàng
            String err = protocol.createAuctionOrError(session);
            if (err == null) {
                lblMessage.setText("");
            } else {
                lblMessage.setText(err);
            }

        } catch (Exception e) {
            lblMessage.setText("Please check the entered information!");
        }
    }

}

