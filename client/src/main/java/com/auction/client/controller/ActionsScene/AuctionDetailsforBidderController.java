package com.auction.client.controller.ActionsScene;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.SessionContext;
import com.auction.client.controller.Card.BidderHistoryCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AutoBidConfig;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller cho màn chi tiết auction (bidder view).
 *
 * <p>Màn này có nhiều tính năng realtime:</p>
 * <ul>
 *   <li>Bind thông tin {@link AuctionSession} vào UI labels và countdown</li>
 *   <li>Đặt bid thủ công và cấu hình auto-bid qua socket protocol</li>
 *   <li>Render bid history cards và LineChart từ backend</li>
 *   <li>Lắng nghe {@link RealtimeAuctionBus} để cập nhật giá/chart/countdown tự động</li>
 *   <li>Hiển thị cảnh báo anti-sniping khi phiên sắp kết thúc hoặc bị server gia hạn</li>
 * </ul>
 *
 * <p><strong>Async strategy:</strong> Tất cả các thao tác network (load bid history, place bid, auto-bid)
 * đều chạy ở background thread để UI không bị đơ khi chờ server.</p>
 */
public class AuctionDetailsforBidderController implements Initializable {

    @FXML private Label lblStatus;
    @FXML private Label lblItemName;
    @FXML private Label lblItemDetails;
    @FXML private Label lblSeller;
    @FXML private TextField txtBidAmount;
    @FXML private HBox btnPlaceBid;
    @FXML private CheckBox chboxAutobid;
    @FXML private VBox paneAutobid;
    @FXML private TextField txtMaxBidAmount;
    @FXML private TextField txtIncrementAmount;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblCurrentPriceSmall;
    @FXML private Label lblStartPrice;
    @FXML private Label lblTimeH;
    @FXML private Label lblTimem;
    @FXML private Label lblTimes;
    @FXML private Label lblCurrentBids;
    @FXML private LineChart<String, Number> lcPriceHistory;
    @FXML private VBox containerBidHistory;
    @FXML private Label lblWarning;
    @FXML private ImageView imgProduct;


    private AuctionSession session;
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private Timeline countdownTimer;
    private Consumer<AuctionSession> realtimeListener;
    private LocalDateTime lastKnownEndTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo trạng thái ban đầu
        if (paneAutobid != null) {
            paneAutobid.setDisable(true);
        }
        showLoadingState();
        loadBidHistoryAsync();
    }

    /* Kiểm tra trạng thái autobid
    private void loadExistingAutoBidConfig() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null || session == null) return;

        // Chạy async để lấy config cũ nếu có
        FxAsync.run("get-autobid-" + session.getId(),
                () -> protocol.getAutoBidConfig(session.getId(), currentUser.getId()),
                config -> {
                    if (config != null) {
                        // Nếu có config, hiển thị lên UI
                        chboxAutobid.setSelected(true);
                        paneAutobid.setDisable(false);
                        txtMaxBidAmount.setText(String.valueOf(config.getMaxAmount()));
                        txtIncrementAmount.setText(String.valueOf(config.getIncrement()));
                    }
                },
                error -> { Không có config hoặc lỗi, giữ nguyên mặc định }); */
    @FXML
    public void handleCfAutoBid(MouseEvent event) {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null || session == null) {
            showAlert("Error", "Please login to use Auto-bid.");
            return;
        }
        try {
            double maxAmount = Double.parseDouble(txtMaxBidAmount.getText().trim());
            double increment = Double.parseDouble(txtIncrementAmount.getText().trim());
            // Validate dữ liệu
            if (increment <= 0) {
                showAlert("Warning", "Increment must be greater than 0.");
                return;
            }
            if (maxAmount <= session.getCurrentPrice()) {
                showAlert("Warning", "Max Bid must be higher than current price.");
                return;
            }

            AutoBidConfig config = new AutoBidConfig(0, currentUser.getId(), session.getId(), maxAmount, increment);
            // Gọi Async để lưu cấu hình
            FxAsync.run("save-autobid",
                    () -> protocol.registerAutoBid(config),
                    success -> {
                        if (Boolean.TRUE.equals(success)) {
                            showAlert("Success", "Auto-bid configuration saved and activated!");
                        } else {
                            showAlert("Error", "Failed to save Auto-bid settings.");
                        }
                    },
                    error -> showAlert("Error", "Network error: " + error.getMessage())
            );

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid numbers for Auto-bid settings.");
        }
    }

    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        if (session == null) return;

        this.lastKnownEndTime = session.getEndTime();
        bindBasicInfo();
        
        // Load bid history async để không block UI khi mở dialog
        loadBidHistoryAsync();
        updateProductImage();
        startCountdownTimer();
        setupRealtimeListener();
    }

    private void bindBasicInfo() {
        if (lblItemName != null && session.getItem() != null) {
            lblItemName.setText(session.getItem().getName());
        }
        if (lblItemDetails != null && session.getItem() != null) {
            lblItemDetails.setText(session.getItem().getDescription());
        }
        if (lblSeller != null && session.getSeller() != null) {
            lblSeller.setText(session.getSeller().getUsername());
        }
        updateStatus();
        updatePriceLabels(session.getCurrentPrice());

        if (lblStartPrice != null) {
            lblStartPrice.setText(String.format("$%,.2f", session.getStartingPrice()));
        }
        if (lblCurrentBids != null) {
            lblCurrentBids.setText(String.valueOf(session.getBids().size()));
        }
    }

    private void updateStatus() {
        if (session == null || lblStatus == null) {
            return;
        }
        switch (session.getStatus()) {
            case OPEN -> {
                lblStatus.setText("COMING");
                lblStatus.setStyle("-fx-background-color: #e6e64c; -fx-text-fill: #8f8f03; -fx-border-color: #8f8f03; -fx-background-radius: 20; -fx-border-radius: 20 ");
            }
            case RUNNING -> {
                lblStatus.setText("RUNNING");
                lblStatus.setStyle("-fx-background-color: #388e3c; -fx-text-fill: #115214; -fx-border-color: #115214; -fx-background-radius: 20; -fx-border-radius: 20 ");
            }
            case FINISHED -> {
                lblStatus.setText("ENDED");
                lblStatus.setStyle("-fx-background-color: #c2185b; -fx-text-fill: #780826; -fx-border-color: #780826; -fx-background-radius: 20; -fx-border-radius: 20 ");
            }
            case CANCELED -> {
                lblStatus.setText("CANCELED");
                lblStatus.setStyle("-fx-background-color: #757575; -fx-text-fill: #474141; -fx-border-color: #474141; -fx-background-radius: 20; -fx-border-radius: 20 ");
            }
        }
    }

    private void updatePriceLabels(double price) {
        String formatted = String.format("$%,.2f", price);
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatted);
        }
        if (lblCurrentPriceSmall != null) {
            lblCurrentPriceSmall.setText(formatted);
        }
    }

    //Update image
    private void updateProductImage() {
        if (imgProduct == null || session == null || session.getItem() == null) {
            return;
        }
        String imagePath = session.getItem().getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        try {
            imgProduct.setImage(new Image(imagePath, 648, 380, true, true, true));
        } catch (RuntimeException e) {
            System.err.println("Cannot load product image: " + imagePath);
        }
    }

    /**
     * Load bid history ở background thread để tránh đơ UI khi mở dialog.
     */
    private void loadBidHistoryAsync() {
        if (session == null) return;

        // Hiển thị trạng thái loading
        showLoadingState();

        // Fetch bid history ở background
        FxAsync.run("bid-history-" + session.getId(),
                () -> protocol.getBidHistory(session.getId()),
                bids -> {
                    renderBidHistoryChart(bids);
                    renderBidHistoryCards(bids);
                },
                error -> {
                    System.err.println("Error loading bid history: " + error);
                    clearLoadingState();
                });
    }

    private void showLoadingState() {
        if (containerBidHistory != null) {
            containerBidHistory.getChildren().clear();
            Label loading = new Label("Loading bid history...");
            loading.setStyle("-fx-text-fill: #6b7280;");
            containerBidHistory.getChildren().add(loading);
        }
    }

    private void clearLoadingState() {
        if (containerBidHistory != null) {
            containerBidHistory.getChildren().clear();
        }
    }

    /**
     * Render chart từ danh sách bid đã load.
     */
    private void renderBidHistoryChart(List<Bid> bids) {
        if (lcPriceHistory == null || session == null) return;

        try {
            lcPriceHistory.getData().clear();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Price History");

            List<Bid> chartBids = bids.stream()
                    .filter(bid -> bid.getTime() != null)
                    .sorted((b1, b2) -> b1.getTime().compareTo(b2.getTime()))
                    .toList();

            // Thêm giá khởi điểm làm baseline; prefix số thứ tự để CategoryAxis không bị trùng label.
            series.getData().add(new XYChart.Data<>("00 Start", session.getStartingPrice()));

            for (int i = 0; i < chartBids.size(); i++) {
                Bid bid = chartBids.get(i);
                String label = String.format("%02d %s", i + 1,
                        bid.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                series.getData().add(new XYChart.Data<>(label, bid.getAmount()));
            }

            lcPriceHistory.getData().add(series);
            lcPriceHistory.setCreateSymbols(true);
            lcPriceHistory.setLegendVisible(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Render bid history cards từ danh sách bid đã load.
     */
    private void renderBidHistoryCards(List<Bid> bids) {
        if (containerBidHistory == null) return;

        containerBidHistory.getChildren().clear();
        try {
            // Sắp xếp bid mới nhất lên đầu; null time được đưa xuống cuối để tránh crash.
            bids.sort((b1, b2) -> {
                if (b1.getTime() == null && b2.getTime() == null) return 0;
                if (b1.getTime() == null) return 1;
                if (b2.getTime() == null) return -1;
                return b2.getTime().compareTo(b1.getTime());
            });

            for (Bid bid : bids) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/BidderHistoryCard.fxml"));
                Node card = loader.load();
                BidderHistoryCardController ctrl = loader.getController();
                ctrl.setBid(bid);
                containerBidHistory.getChildren().add(card);
            }
        } catch (IOException e) {
            System.err.println("Error loading BidderHistoryCards: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startCountdownTimer() {
        if (session == null) return;

        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeLabels();
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();

        updateTimeLabels();
    }

    private void updateTimeLabels() {
        if (session == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime();

        long seconds = ChronoUnit.SECONDS.between(now, end);

        if (seconds <= 0) {
            if (lblTimeH != null) lblTimeH.setText("00");
            if (lblTimem != null) lblTimem.setText("00");
            if (lblTimes != null) lblTimes.setText("00");
            if (lblWarning != null) {
                lblWarning.setText("Auction ended. Waiting for final result update...");
                lblWarning.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
            }
            if (countdownTimer != null) countdownTimer.stop();
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            long secs = seconds % 60;

            if (lblTimeH != null) lblTimeH.setText(String.format("%03d", hours));
            if (lblTimem != null) lblTimem.setText(String.format("%02d", mins));
            if (lblTimes != null) lblTimes.setText(String.format("%02d", secs));
            updateAntiSnipeWarning(seconds);
        }
    }

    private void updateAntiSnipeWarning(long secondsRemaining) {
        if (lblWarning == null) return;
        if (secondsRemaining <= 30) {
            lblWarning.setText("⚠️ Last 30 seconds: a new bid can extend this auction by 60 seconds.");
            lblWarning.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
        } else if (secondsRemaining <= 300) {
            lblWarning.setText("Auction ending soon. Prepare your next bid.");
            lblWarning.setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
        } else {
            lblWarning.setText("");
        }
    }

    private void setupRealtimeListener() {
        if (realtimeListener != null) return;
        realtimeListener = updatedSession -> {
            if (session == null || updatedSession == null || updatedSession.getId() != session.getId()) {
                return;
            }
            LocalDateTime previousEnd = lastKnownEndTime;
            session = updatedSession;
            lastKnownEndTime = updatedSession.getEndTime();
            bindBasicInfo();
            loadBidHistoryAsync();
            updateTimeLabels();
            if (lblWarning != null && previousEnd != null && updatedSession.getEndTime() != null
                    && updatedSession.getEndTime().isAfter(previousEnd)) {
                lblWarning.setText("Anti-sniping activated: auction extended by server.");
                lblWarning.setStyle("-fx-text-fill: #047857; -fx-font-weight: bold;");
            }
        };
        RealtimeAuctionBus.addAuctionListener(realtimeListener);
    }

    @FXML
    public void handlePlaceBid(MouseEvent event) {
        if (session == null) return;

        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) {
            showAlert("Error", "Please login first!");
            return;
        }

        boolean autoBidEnabled = chboxAutobid != null && chboxAutobid.isSelected();
        double bidAmount;
        if (autoBidEnabled) {
            Double preparedAutoBid = prepareAutoBidStartAmount();
            if (preparedAutoBid == null) {
                return;
            }
            bidAmount = preparedAutoBid;
        } else {
            String amountText = txtBidAmount.getText();
            if (amountText == null || amountText.trim().isEmpty()) {
                showAlert("Warning", "Please enter a bid amount.");
                return;
            }
            try {
                bidAmount = Double.parseDouble(amountText.trim());
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid bid amount format.");
                return;
            }
        }

        if (bidAmount <= session.getCurrentPrice()) {
            showAlert("Warning", "Bid amount must be strictly greater than the current price!");
            return;
        }

        // Disable button để tránh double-click khi đang gửi bid/autobid lên server.
        btnPlaceBid.setDisable(true);

        // Gọi backend ở background thread.
        FxAsync.run("place-bid-" + session.getId(),
                () -> protocol.placeBidOrError(session.getId(), currentUser.getId(), bidAmount),
                errorMsg -> {
                    if (errorMsg != null) {
                        showAlert("Error", errorMsg);
                        btnPlaceBid.setDisable(false);
                        return;
                    }

                    // Nếu auto-bid được chọn, đăng ký config ngay sau bid mở đầu thành công.
                    if (autoBidEnabled) {
                        registerAutoBidAsync(currentUser, bidAmount);
                    } else {
                        txtBidAmount.clear();
                        refreshAuctionDataAsync();
                        showAlert("Success", "Bid placed successfully!");
                        btnPlaceBid.setDisable(false);
                    }
                });
    }

    private Double prepareAutoBidStartAmount() {
        try {
            double maxAmount = Double.parseDouble(txtMaxBidAmount.getText().trim());
            double increment = Double.parseDouble(txtIncrementAmount.getText().trim());
            double nextBid = Math.min(session.getCurrentPrice() + increment, maxAmount);

            if (increment <= 0) {
                showAlert("Warning", "Auto-bid increment must be greater than 0.");
                return null;
            }
            if (maxAmount <= session.getCurrentPrice()) {
                showAlert("Warning", "Maximum bid must be greater than the current price.");
                return null;
            }
            if (nextBid <= session.getCurrentPrice()) {
                showAlert("Warning", "Auto-bid cannot start because the next bid is not high enough.");
                return null;
            }
            // Điền bid mở đầu để người dùng nhìn thấy số tiền sẽ được gửi khi bấm nút start/place bid.
            txtBidAmount.setText(String.valueOf(nextBid));
            return nextBid;
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid maximum bid and increment values.");
            return null;
        }
    }

    /**
     * Đăng ký auto-bid ở background thread.
     */
    private void registerAutoBidAsync(User currentUser, double initialBidAmount) {
        try {
            double maxAmount = Double.parseDouble(txtMaxBidAmount.getText().trim());
            double increment = Double.parseDouble(txtIncrementAmount.getText().trim());

            if (maxAmount <= initialBidAmount || increment <= 0) {
                txtBidAmount.clear();
                refreshAuctionDataAsync();
                showAlert("Success", "Bid placed successfully!");
                btnPlaceBid.setDisable(false);
                return;
            }

            AutoBidConfig config = new AutoBidConfig(0, currentUser.getId(), session.getId(), maxAmount, increment);
            FxAsync.run("auto-bid-" + session.getId(),
                    () -> protocol.registerAutoBid(config),
                    success -> {
                        txtBidAmount.clear();
                        refreshAuctionDataAsync();
                        if (Boolean.TRUE.equals(success)) {
                            showAlert("Success", "Auto-bid started successfully!");
                        } else {
                            showAlert("Warning", "Bid placed, but auto-bid could not be started.");
                        }
                        btnPlaceBid.setDisable(false);
                    },
                    error -> {
                        txtBidAmount.clear();
                        refreshAuctionDataAsync();
                        showAlert("Warning", "Bid placed, but auto-bid could not be started: " + error.getMessage());
                        btnPlaceBid.setDisable(false);
                    });
        } catch (NumberFormatException e) {
            txtBidAmount.clear();
            refreshAuctionDataAsync();
            showAlert("Success", "Bid placed successfully!");
            btnPlaceBid.setDisable(false);
        }
    }

    @FXML
    public void hanldeAutoBid(ActionEvent event) {
        boolean autoEnabled = chboxAutobid.isSelected();
        paneAutobid.setDisable(!autoEnabled);
        
        if (!autoEnabled) {
            txtMaxBidAmount.clear();
            txtIncrementAmount.clear();
            
            // Hủy auto bid trên server ở background
            User u = SessionContext.getCurrentUser();
            if (u != null && session != null) {
                FxAsync.run("cancel-auto-bid-" + session.getId(),
                        () -> {
                            protocol.cancelAutoBid(session.getId(), u.getId());
                            return null;
                        },
                        result -> {
                            // Auto-bid đã được hủy
                        });
            }
        }
    }

    /**
     * Refresh dữ liệu auction từ server ở background thread.
     */
    private void refreshAuctionDataAsync() {
        if (session == null) return;
        
        FxAsync.run("refresh-auction-" + session.getId(),
                protocol::getActiveAuctions,
                activeAuctions -> {
                    for (AuctionSession s : activeAuctions) {
                        if (s.getId() == session.getId()) {
                            this.session = s;
                            bindBasicInfo();
                            loadBidHistoryAsync();
                            break;
                        }
                    }
                });
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (realtimeListener != null) {
            RealtimeAuctionBus.removeAuctionListener(realtimeListener);
            realtimeListener = null;
        }
    }
}
