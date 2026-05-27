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

    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        if (session == null) return;

        this.lastKnownEndTime = session.getEndTime();
        bindBasicInfo();
        
        // Load bid history async để không block UI khi mở dialog
        loadBidHistoryAsync();
        
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

            // Thêm giá khởi điểm làm baseline
            series.getData().add(new XYChart.Data<>("Start", session.getStartingPrice()));

            // Format thời gian hoặc index tuần tự
            for (int i = 0; i < bids.size(); i++) {
                Bid bid = bids.get(i);
                String label;
                if (bid.getTime() != null) {
                    label = bid.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                } else {
                    label = "Bid #" + (i + 1);
                }
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
            // Sắp xếp bid mới nhất lên đầu
            bids.sort((b1, b2) -> b2.getTime().compareTo(b1.getTime()));

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

        String amountText = txtBidAmount.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            showAlert("Warning", "Please enter a bid amount.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid bid amount format.");
            return;
        }

        if (bidAmount <= session.getCurrentPrice()) {
            showAlert("Warning", "Bid amount must be strictly greater than the current price!");
            return;
        }

        // Disable button để tránh double-click
        btnPlaceBid.setDisable(true);

        // Gọi backend ở background thread
        FxAsync.run("place-bid-" + session.getId(),
                () -> protocol.placeBidOrError(session.getId(), currentUser.getId(), bidAmount),
                errorMsg -> {
                    if (errorMsg != null) {
                        showAlert("Error", errorMsg);
                        btnPlaceBid.setDisable(false);
                        return;
                    }

                    // Nếu autobid được chọn, đăng ký nó
                    if (chboxAutobid.isSelected()) {
                        registerAutoBidAsync(currentUser, bidAmount);
                    } else {
                        // Refresh UI và hiển thị thông báo thành công
                        txtBidAmount.clear();
                        refreshAuctionDataAsync();
                        showAlert("Success", "Bid placed successfully!");
                        btnPlaceBid.setDisable(false);
                    }
                });
    }

    /**
     * Đăng ký auto-bid ở background thread.
     */
    private void registerAutoBidAsync(User currentUser, double initialBidAmount) {
        try {
            double maxAmount = Double.parseDouble(txtMaxBidAmount.getText().trim());
            double increment = Double.parseDouble(txtIncrementAmount.getText().trim());
            
            if (maxAmount > initialBidAmount && increment > 0) {
                AutoBidConfig config = new AutoBidConfig(0, currentUser.getId(), session.getId(), maxAmount, increment);
                
                FxAsync.run("auto-bid-" + session.getId(),
                        () -> {
                            protocol.registerAutoBid(config);
                            return null;
                        },
                        result -> {
                            txtBidAmount.clear();
                            refreshAuctionDataAsync();
                            showAlert("Success", "Bid placed successfully with auto-bid enabled!");
                            btnPlaceBid.setDisable(false);
                        });
            } else {
                txtBidAmount.clear();
                refreshAuctionDataAsync();
                showAlert("Success", "Bid placed successfully!");
                btnPlaceBid.setDisable(false);
            }
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
