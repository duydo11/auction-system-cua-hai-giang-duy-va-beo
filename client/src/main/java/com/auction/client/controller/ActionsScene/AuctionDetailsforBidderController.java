package com.auction.client.controller.ActionsScene;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AuctionDetailsForBidder — hiển thị chi tiết phiên đấu giá với Bid History Chart.
 * 
 * Giang cần thêm fx:id vào FXML:
 * - lblItemName (tên sản phẩm)
 * - lblDescription (mô tả)
 * - lblSeller (tên seller)
 * - lblStatus (trạng thái)
 * - lblCurrentPrice (giá hiện tại)
 * - lblStartingPrice (giá khởi điểm)
 * - lblTimeRemaining (thời gian còn lại - 3 labels: hours, mins, secs)
 * - lblBidsCount (số lượng bids)
 * - txtBidAmount (input bid amount)
 * - chartBidHistory (LineChart - đã có trong FXML line 384)
 */
public class AuctionDetailsforBidderController implements Initializable {
    
    // Product info
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblSeller;
    @FXML private Label lblStatus;
    
    // Bid info
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblBidsCount;
    @FXML private TextField txtBidAmount;
    
    // Time remaining (3 labels: HH:MM:SS)
    @FXML private Label lblTimeHours;
    @FXML private Label lblTimeMins;
    @FXML private Label lblTimeSecs;
    
    // Bid History Chart
    @FXML private LineChart<Number, Number> chartBidHistory;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    
    private AuctionSession session;
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private Timeline countdownTimer;
    
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Will be set via setAuctionSession() from caller
    }
    
    /**
     * Set auction session và load tất cả data.
     * Gọi từ ProductCard khi click "View Details".
     */
    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        
        if (session == null) {
            return;
        }
        
        // Bind basic info
        bindBasicInfo();
        
        // Load bid history và vẽ chart
        loadBidHistoryChart();
        
        // Start countdown timer
        startCountdownTimer();
    }
    
    /**
     * Bind thông tin cơ bản.
     */
    private void bindBasicInfo() {
        if (lblItemName != null && session.getItem() != null) {
            lblItemName.setText(session.getItem().getName());
        }
        
        if (lblDescription != null && session.getItem() != null) {
            lblDescription.setText(session.getItem().getDescription());
        }
        
        if (lblSeller != null && session.getSeller() != null) {
            lblSeller.setText(session.getSeller().getUsername());
        }
        
        if (lblStatus != null) {
            updateStatus();
        }
        
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%.0f VND", session.getCurrentPrice()));
        }
        
        if (lblStartingPrice != null) {
            lblStartingPrice.setText(String.format("%.0f VND", session.getStartingPrice()));
        }
        
        if (lblBidsCount != null) {
            lblBidsCount.setText(String.valueOf(session.getBids().size()));
        }
    }
    
    /**
     * Update status badge.
     */
    private void updateStatus() {
        if (session == null || lblStatus == null) {
            return;
        }
        
        switch (session.getStatus()) {
            case OPEN -> lblStatus.setText("Chưa bắt đầu");
            case RUNNING -> lblStatus.setText("Đang đấu giá");
            case FINISHED -> lblStatus.setText("Đã kết thúc");
            case CANCELED -> lblStatus.setText("Đã hủy");
        }
    }
    
    /**
     * Load bid history và vẽ LineChart.
     * Đây là phần quan trọng nhất — Bid History Visualization (0.5 điểm).
     */
    private void loadBidHistoryChart() {
        if (chartBidHistory == null || session == null) {
            return;
        }
        
        try {
            // Get bid history từ backend
            List<Bid> bids = protocol.getBidHistory(session.getId());
            
            if (bids.isEmpty()) {
                // No bids yet — show starting price as baseline
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Giá đấu");
                series.getData().add(new XYChart.Data<>(0, session.getStartingPrice()));
                chartBidHistory.getData().add(series);
                return;
            }
            
            // Create series
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Giá đấu");
            
            // Add starting price as first point
            series.getData().add(new XYChart.Data<>(0, session.getStartingPrice()));
            
            // Add all bids
            LocalDateTime startTime = session.getStartTime();
            for (Bid bid : bids) {
                // X-axis: seconds since auction start
                long secondsSinceStart = ChronoUnit.SECONDS.between(startTime, bid.getTime());
                
                // Y-axis: bid amount
                double amount = bid.getAmount();
                
                series.getData().add(new XYChart.Data<>(secondsSinceStart, amount));
            }
            
            // Add series to chart
            chartBidHistory.getData().clear();
            chartBidHistory.getData().add(series);
            
            // Configure axes
            if (xAxis != null) {
                xAxis.setLabel("Thời gian (giây)");
            }
            if (yAxis != null) {
                yAxis.setLabel("Giá (VND)");
                yAxis.setAutoRanging(true);
            }
            
            // Style
            chartBidHistory.setLegendVisible(false);
            chartBidHistory.setCreateSymbols(true); // Show dots at each bid
            
        } catch (Exception e) {
            System.err.println("Error loading bid history chart: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Countdown timer — update mỗi giây.
     */
    private void startCountdownTimer() {
        if (session == null) {
            return;
        }
        
        // Stop old timer if exists
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeRemaining();
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
        
        // Initial update
        updateTimeRemaining();
    }
    
    /**
     * Update time remaining labels (HH:MM:SS).
     */
    private void updateTimeRemaining() {
        if (session == null) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime();
        
        long seconds = ChronoUnit.SECONDS.between(now, end);
        
        if (seconds < 0) {
            // Ended
            if (lblTimeHours != null) lblTimeHours.setText("00");
            if (lblTimeMins != null) lblTimeMins.setText("00");
            if (lblTimeSecs != null) lblTimeSecs.setText("00");
            
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            long secs = seconds % 60;
            
            if (lblTimeHours != null) lblTimeHours.setText(String.format("%03d", hours));
            if (lblTimeMins != null) lblTimeMins.setText(String.format("%02d", mins));
            if (lblTimeSecs != null) lblTimeSecs.setText(String.format("%02d", secs));
        }
    }
    
    /**
     * Handle "Place Bid" button.
     * Giang cần wire button: onAction="#handlePlaceBid"
     */
    @FXML
    private void handlePlaceBid() {
        if (session == null || txtBidAmount == null) {
            return;
        }
        
        String amountStr = txtBidAmount.getText().trim().replace(",", "");
        if (amountStr.isEmpty()) {
            showError("Nhập số tiền đặt giá");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ");
            return;
        }
        
        if (amount <= session.getCurrentPrice()) {
            showError("Giá phải cao hơn giá hiện tại");
            return;
        }
        
        // TODO: Get current user ID
        int bidderId = 1; // Placeholder — Giang cần lấy từ SessionContext
        
        // Call backend
        String err = protocol.placeBidOrError(session.getId(), bidderId, amount);
        
        if (err == null) {
            // Success — reload chart
            loadBidHistoryChart();
            txtBidAmount.clear();
            showSuccess("Đặt giá thành công!");
        } else {
            showError(err);
        }
    }
    
    /**
     * Show error message.
     * Giang cần thêm Label lblMessage vào FXML.
     */
    private void showError(String message) {
        System.err.println("Error: " + message);
        // TODO: Giang thêm Label lblMessage để hiển thị
    }
    
    /**
     * Show success message.
     */
    private void showSuccess(String message) {
        System.out.println("Success: " + message);
        // TODO: Giang thêm Label lblMessage để hiển thị
    }
    
    /**
     * Cleanup khi đóng dialog.
     */
    public void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }
}
