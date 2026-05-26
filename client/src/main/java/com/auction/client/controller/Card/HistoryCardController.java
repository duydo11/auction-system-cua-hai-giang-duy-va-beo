package com.auction.client.controller.Card;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.auction.Bid;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * History card controller backed by a bid and its session.
 */
public class HistoryCardController {

    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblStatus;
    @FXML private Label lblStatusDetails;
    @FXML private ImageView imgProduct;
    @FXML private Button btnViewDetails;

    private Bid bid;
    private AuctionSession session;
    private Timeline countdownTimer;

    public void setBid(Bid bid) {
        this.bid = bid;
        this.session = bid != null ? bid.getAuctionSession() : null;
        if (bid == null || session == null) {
            return;
        }

        if (lblItemName != null && session.getItem() != null) {
            lblItemName.setText(session.getItem().getName());
        }
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%.0f VND", bid.getAmount()));
        }
        if (lblStatusDetails != null && bid.getTime() != null) {
            lblStatusDetails.setText("Bid at: " + bid.getTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        }
        updateStatus();
        updateProductImage();
        startCountdownTimer();
    }

    private void updateStatus() {
        if (lblStatus == null || session == null) {
            return;
        }
        AuctionStatus status = session.getStatus();
        if (status == null) {
            lblStatus.setText("UNKNOWN");
            return;
        }
        switch (status) {
            case OPEN -> lblStatus.setText("COMING");
            case RUNNING -> lblStatus.setText("RUNNING");
            case FINISHED -> lblStatus.setText("FINISHED");
            case CANCELED -> lblStatus.setText("CANCELED");
        }
    }

    private void updateProductImage() {
        if (imgProduct == null || session == null || session.getItem() == null) {
            return;
        }
        String imagePath = session.getItem().getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        try {
            imgProduct.setImage(new Image(imagePath, true));
        } catch (RuntimeException e) {
            System.err.println("Cannot load history image: " + imagePath);
        }
    }

    private void startCountdownTimer() {
        if (lblTimeRemaining == null || session == null) {
            return;
        }
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTimeRemaining()));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
        updateTimeRemaining();
    }

    private void updateTimeRemaining() {
        if (lblTimeRemaining == null || session == null || session.getEndTime() == null) {
            return;
        }
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getEndTime());
        if (seconds < 0) {
            lblTimeRemaining.setText("Ended");
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            return;
        }
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        lblTimeRemaining.setText(String.format("%dh %dm", hours, mins));
    }

    @FXML
    private void handleViewDetails() {
        System.out.println("History bid details already shown.");
    }
}
