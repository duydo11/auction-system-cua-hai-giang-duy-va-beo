package com.auction.client.controller.Card;

import com.auction.shared.model.auction.AuctionSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Controller cho ProductCard — bind data từ AuctionSession.
 * Giang cần thêm fx:id vào FXML:
 * - lblItemName (tên sản phẩm)
 * - lblCurrentPrice (giá hiện tại)
 * - lblTimeRemaining (thời gian còn lại)
 * - lblStatus (trạng thái: OPEN/RUNNING/FINISHED)
 * - btnViewDetails (nút xem chi tiết)
 */
public class ProductCardController {

    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblStatus;
    @FXML private Button btnViewDetails;

    private AuctionSession session;
    private Timeline countdownTimer;

    /**
     * Gọi từ BidderDashboard sau khi load FXML.
     * Bind data từ AuctionSession vào UI.
     */
    public void setAuctionSession(AuctionSession session) {
        this.session = session;

        if (session == null) {
            return;
        }

        // Bind item name
        if (lblItemName != null && session.getItem() != null) {
            lblItemName.setText(session.getItem().getName());
        }

        // Bind current price
        if (lblCurrentPrice != null) {
            updatePrice(session.getCurrentPrice());
        }

        // Bind status
        if (lblStatus != null) {
            updateStatus();
        }

        // Start countdown timer
        startCountdownTimer();
    }

    /**
     * Update giá khi có bid mới (gọi từ RealtimeAuctionBus).
     */
    public void updatePrice(double newPrice) {
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%.0f VND", newPrice));
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
            case OPEN -> {
                lblStatus.setText("Chưa bắt đầu");
                lblStatus.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2;");
            }
            case RUNNING -> {
                lblStatus.setText("Đang đấu giá");
                lblStatus.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #388e3c;");
            }
            case FINISHED -> {
                lblStatus.setText("Đã kết thúc");
                lblStatus.setStyle("-fx-background-color: #fce4ec; -fx-text-fill: #c2185b;");
            }
            case CANCELED -> {
                lblStatus.setText("Đã hủy");
                lblStatus.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #757575;");
            }
        }
    }

    /**
     * Countdown timer — update mỗi giây.
     */
    private void startCountdownTimer() {
        if (session == null || lblTimeRemaining == null) {
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
     * Update time remaining label.
     */
    private void updateTimeRemaining() {
        if (session == null || lblTimeRemaining == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime();

        long seconds = ChronoUnit.SECONDS.between(now, end);

        if (seconds < 0) {
            lblTimeRemaining.setText("Đã kết thúc");
            lblTimeRemaining.setStyle("-fx-text-fill: #c62828;");
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
        } else if (seconds < 300) {
            // < 5 phút: hiển thị đỏ cảnh báo
            long mins = seconds / 60;
            long secs = seconds % 60;
            lblTimeRemaining.setText(String.format("⚠️ %d:%02d", mins, secs));
            lblTimeRemaining.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        } else {
            // Bình thường
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            lblTimeRemaining.setText(String.format("%dh %dm", hours, mins));
            lblTimeRemaining.setStyle("-fx-text-fill: #424242;");
        }
    }

    /**
     * Cleanup khi card bị remove.
     */
    public void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    /**
     * Handle "View Details" button click.
     * Giang cần wire button trong FXML: onAction="#handleViewDetails"
     */
    @FXML
    private void handleViewDetails() {
        if (session == null) {
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ActionsScene/AuctionDetailsforBidder.fxml"));
            javafx.scene.Parent root = loader.load();

            com.auction.client.controller.ActionsScene.AuctionDetailsforBidderController controller = loader.getController();
            controller.setAuctionSession(session);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Chi tiết phiên đấu giá: " + (session.getItem() != null ? session.getItem().getName() : ""));
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            // Clean up when dialog closes
            dialogStage.setOnCloseRequest(event -> controller.cleanup());

            dialogStage.showAndWait();

        } catch (java.io.IOException e) {
            System.err.println("Error loading details dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public AuctionSession getSession() {
        return session;
    }
}
