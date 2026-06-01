package com.auction.client.controller.ActionsScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemFactory;
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
import javafx.scene.layout.HBox;
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
 *
 * <p>Thao tác tạo auction chạy async để UI không bị đơ khi chờ server.</p>
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
    @FXML private HBox btnConfirm;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    /** Đường dẫn file ảnh đã chọn (null nếu chưa chọn). */
    private File selectedImageFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbCategory.getItems().addAll("Arts", "Vehicles", "Electronics", "Others");

        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 1; i <= 12; i++) hours.add(String.format("%02d", i));
        cbStartHour.setItems(hours);
        cbEndHour.setItems(hours);

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) minutes.add(String.format("%02d", i));
        cbStartMin.setItems(minutes);
        cbEndMin.setItems(minutes);

        ObservableList<String> ampm = FXCollections.observableArrayList("AM", "PM");
        cbStartAMPM.setItems(ampm);
        cbEndAMPM.setItems(ampm);

        txtStartingPrice.setPromptText("Enter numbers only (x.000 VND)");
    }

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

    @FXML
    private void handleChooseImage(MouseEvent mouseEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose product image");
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

    @FXML
    private void handleCreateAuction(MouseEvent mouseEvent) {
        try {
            String name = txtProductName.getText().trim();
            String desc = txtProductDetails.getText().trim();
            String priceStr = txtStartingPrice.getText().trim().replace(",", "");

            if (priceStr.isEmpty()) {
                lblMessage.setText("Please enter a starting price.");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                lblMessage.setText("Invalid number. Starting price must contain digits only.");
                return;
            }
            if (price <= 0) {
                lblMessage.setText("Starting price must be greater than 0.");
                return;
            }

            LocalDateTime start = combineDateTime(dpStartDate, cbStartHour, cbStartMin, cbStartAMPM);
            LocalDateTime end = combineDateTime(dpEndDate, cbEndHour, cbEndMin, cbEndAMPM);

            if (end.isBefore(start)) {
                lblMessage.setText("End date must be after start date!");
                return;
            }

            User u = SessionContext.getCurrentUser();
            if (!(u instanceof Seller seller)) {
                lblMessage.setText("You are not in seller mode. Please switch to the Seller tab first.");
                return;
            }
            if (name.isEmpty()) {
                lblMessage.setText("Please enter a product name.");
                return;
            }

            String selectedType = cbCategory.getValue();
            if (selectedType == null) {
                lblMessage.setText("Vui lòng chọn loại sản phẩm.");
                return;
            }

            Item item;
            try {
                item = ItemFactory.create(selectedType, 0, name, desc, seller, null);
            } catch (IllegalArgumentException e) {
                lblMessage.setText("Lỗi loại sản phẩm: " + e.getMessage());
                return;
            }
            if (selectedImageFile != null) {
                item.setImagePath(selectedImageFile.toURI().toString());
            }
            AuctionSession session = new AuctionSession(0, seller, item, price, start, end);

            if (btnConfirm != null) {
                btnConfirm.setDisable(true);
            }

            lblMessage.setStyle("-fx-text-fill: #1976d2;");
            lblMessage.setText("Creating auction...");

            // Không dùng local fallback fake: phải lưu lên server thành công thì mới báo tạo thành công.
            FxAsync.run("create-auction",
                    () -> protocol.createAuctionOrError(session),
                    err -> {
                        if (err == null) {
                            // Tạo thành công thì xoá cache để các màn sau load lại dữ liệu thật từ server.
                            AuctionCache.invalidate();
                            lblMessage.setStyle("-fx-text-fill: #2e7d32;");
                            lblMessage.setText("Tạo phiên đấu giá thành công.");
                            lblMessage.getScene().getWindow().hide();
                        } else {
                            lblMessage.setStyle("-fx-text-fill: #c62828;");
                            lblMessage.setText(err);
                        }

                        if (btnConfirm != null) {
                            btnConfirm.setDisable(false);
                        }
                    },
                    error -> {
                        lblMessage.setStyle("-fx-text-fill: #c62828;");
                        lblMessage.setText("Error: " + error);

                        if (btnConfirm != null) {
                            btnConfirm.setDisable(false);
                        }
                    });

        } catch (Exception e) {
            lblMessage.setText("Please review the form and try again.");
        }
    }
}
