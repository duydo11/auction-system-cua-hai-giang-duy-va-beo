package com.auction.client.controller.Card;

import com.auction.shared.model.auction.AuctionSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Controller cho ProductCard — bind data từ AuctionSession.
 */
public class ProductCardController {

    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblStartTime;
    @FXML private Label lblStatus;
    @FXML private ImageView imgProduct;
    @FXML private Button btnViewDetails;

    private AuctionSession session;
    private Timeline countdownTimer;

    public void setAuctionSession(AuctionSession session) {
        this.session = session;

        if (session == null) {
            return;
        }

        if (lblItemName != null && session.getItem() != null) {
            lblItemName.setText(session.getItem().getName());
        }

        if (lblCurrentPrice != null) {
            updatePrice(session.getCurrentPrice());
        }

        if (lblStatus != null) {
            updateStatus();
        }

        updateStartTime();
        updateProductImage();
        startCountdownTimer();
    }

    public void updatePrice(double newPrice) {
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("%,.0f $", newPrice));
        }
    }

    private void updateStatus() {
        if (session == null || lblStatus == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (session.getStartTime() != null && now.isBefore(session.getStartTime())) {
            lblStatus.setText("COMING");
            lblStatus.setStyle("-fx-background-color: #e6e64c; -fx-text-fill: #8f8f03; -fx-border-color: #8f8f03; -fx-background-radius: 20; -fx-border-radius: 20 ");
            return;
        }
        if (session.getEndTime() != null && now.isAfter(session.getEndTime())) {
            lblStatus.setText("ENDED");
            lblStatus.setStyle("-fx-background-color: #c2185b; -fx-text-fill: #780826; -fx-border-color: #780826; -fx-background-radius: 20; -fx-border-radius: 20 ");
            return;
        }
        if (session.getStatus() == com.auction.shared.model.auction.AuctionStatus.CANCELED) {
            lblStatus.setText("CANCELED");
            lblStatus.setStyle("-fx-background-color: #757575; -fx-text-fill: #474141; -fx-border-color: #474141; -fx-background-radius: 20; -fx-border-radius: 20 ");
            return;
        }
        lblStatus.setText("RUNNING");
        lblStatus.setStyle("-fx-background-color: #388e3c; -fx-text-fill: #115214; -fx-border-color: #115214; -fx-background-radius: 20; -fx-border-radius: 20 ");
    }

    private void updateStartTime() {
        if (lblStartTime != null && session != null && session.getStartTime() != null) {
            lblStartTime.setText("Start: " + session.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")));
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
            imgProduct.setImage(new Image(imagePath, 220, 140, true, true, true));
        } catch (RuntimeException e) {
            System.err.println("Cannot load product image: " + imagePath);
        }
    }

    private void startCountdownTimer() {
        if (session == null || lblTimeRemaining == null) {
            return;
        }

        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateTimeRemaining();
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
        updateTimeRemaining();
    }

    private void updateTimeRemaining() {
        if (session == null || lblTimeRemaining == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime();

        long seconds = ChronoUnit.SECONDS.between(now, end);

        if (seconds < 0) {
            lblTimeRemaining.setText("ENDED");
            lblTimeRemaining.setStyle("-fx-text-fill: #c62828;");
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
        } else if (seconds < 300) {
            long mins = seconds / 60;
            long secs = seconds % 60;
            lblTimeRemaining.setText(String.format("Ending soon %d:%02d", mins, secs));
            lblTimeRemaining.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            lblTimeRemaining.setText(String.format("%dh %dm", hours, mins));
            lblTimeRemaining.setStyle("-fx-text-fill: #424242;");
        }
    }

    public void cleanup() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

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
            dialogStage.setTitle("Auction details: " + (session.getItem() != null ? session.getItem().getName() : ""));
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
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
