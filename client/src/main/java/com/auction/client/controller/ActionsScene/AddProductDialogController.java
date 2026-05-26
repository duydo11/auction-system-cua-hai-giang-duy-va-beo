package com.auction.client.controller.ActionsScene;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Controller cho dialog tạo sản phẩm đấu giá mới.
 *
 * <p>Cho phép seller nhập tên, mô tả, chọn category, giá khởi điểm,
 * thời gian bắt đầu/kết thúc, và chọn ảnh sản phẩm từ máy tính.</p>
 */
public class AddProductDialogController implements Initializable {
    @FXML private TextArea txtProductName;
    @FXML private TextArea txtStartingPrice;
    @FXML private TextArea txtProductDetails;
    @FXML private ComboBox<String> cbCategory;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private ComboBox<String> cbStartHour, cbStartMin, cbStartAMPM;
    @FXML private ComboBox<String> cbEndHour, cbEndMin, cbEndAMPM;
    @FXML private Label lblMessage;
    @FXML private ImageView imgPreview;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    /** Đường dẫn file ảnh đã chọn (null nếu chưa chọn). */
    private File selectedImageFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbCategory.getItems().addAll("Arts", "Vehicles", "Electronics", "Others");

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

        // Prompt cho starting price
        txtStartingPrice.setPromptText("Nhập số (x.000 VND)");
    }

    /**
     * Ghép date + hour/minute/ampm thành LocalDateTime.
     */
    private LocalDateTime combineDateTime(DatePicker datePicker, ComboBox<String> hourCB,
                                           ComboBox<String> minCB, ComboBox<String> ampmCB) {
        LocalDate date = datePicker.getValue();
        int hour = Integer.parseInt(hourCB.getValue());
        int minute = Integer.parseInt(minCB.getValue());
        String ampm = ampmCB.getValue();
        if (ampm.equals("PM") && hour < 12) hour += 12;
        if (ampm.equals("AM") && hour == 12) hour = 0;
        return date.atTime(hour, minute);
    }

    /**
     * Mở FileChooser để chọn ảnh sản phẩm (JPG/PNG) từ máy tính.
     */
    @FXML
    private void handleChooseImage(MouseEvent mouseEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(lblMessage.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            if (imgPreview != null) {
                imgPreview.setImage(new Image(file.toURI().toString()));
            }
        }
    }

    /**
     * Tạo phiên đấu giá khi user ấn Confirm.
     */
    @FXML
    private void handleCreateAuction(MouseEvent mouseEvent) {
        try {
            // 1. Lấy thông tin cơ bản
            String name = txtProductName.getText().trim();
            String desc = txtProductDetails.getText().trim();
            String priceStr = txtStartingPrice.getText().trim().replace(",", "");

            // 2. Validate starting price: chỉ được nhập số
            if (priceStr.isEmpty()) {
                lblMessage.setText("Vui lòng nhập giá khởi điểm.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                lblMessage.setText("Invalid number! Chỉ được nhập số cho giá khởi điểm.");
                return;
            }
            if (price <= 0) {
                lblMessage.setText("Giá khởi điểm phải lớn hơn 0.");
                return;
            }

            // 3. Lấy thời gian
            LocalDateTime start = combineDateTime(dpStartDate, cbStartHour, cbStartMin, cbStartAMPM);
            LocalDateTime end = combineDateTime(dpEndDate, cbEndHour, cbEndMin, cbEndAMPM);

            if (end.isBefore(start)) {
                lblMessage.setText("End date must be after start date!");
                return;
            }

            // 4. Kiểm tra user có phải Seller hay không
            User u = SessionContext.getCurrentUser();
            if (!(u instanceof Seller seller)) {
                lblMessage.setText("You are not seller! Hãy chuyển sang tab Seller trước.");
                return;
            }
            if (name.isEmpty()) {
                lblMessage.setText("Vui lòng nhập tên sản phẩm.");
                return;
            }

            // 5. Tạo phiên đấu giá
            Electronics item = new Electronics(0, name, desc, seller, 12);
            AuctionSession session = new AuctionSession(0, seller, item, price, start, end);

            String err = protocol.createAuctionOrError(session);
            if (err == null) {
                lblMessage.setStyle("-fx-text-fill: #2e7d32;");
                lblMessage.setText("Tạo phiên đấu giá thành công!");
            } else {
                lblMessage.setText(err);
            }

        } catch (Exception e) {
            lblMessage.setText("Vui lòng kiểm tra lại thông tin nhập vào!");
        }
    }
}
