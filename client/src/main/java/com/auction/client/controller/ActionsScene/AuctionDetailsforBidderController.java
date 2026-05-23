package com.auction.client.controller.ActionsScene;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.SessionContext;
import com.auction.client.controller.Card.BidderHistoryCardController;
import com.auction.client.network.ClientProtocolHandler;
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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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
 * Controller for the bidder auction detail screen.
 *
 * <p>This screen is where most bidder-side advanced features meet:</p>
 * <ul>
 *   <li>binds a real {@link AuctionSession} to labels and countdown fields,</li>
 *   <li>places manual bids and optional auto-bid configuration through the socket protocol,</li>
 *   <li>renders bid history cards and a JavaFX LineChart from backend bid data,</li>
 *   <li>listens to {@link RealtimeAuctionBus} so price/chart/countdown update without reopening the page,</li>
 *   <li>shows anti-sniping warnings when the session is close to ending or extended by the server.</li>
 * </ul>
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
        // Init state
        if (paneAutobid != null) {
            paneAutobid.setDisable(true);
        }
    }

    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        if (session == null) return;

        this.lastKnownEndTime = session.getEndTime();
        bindBasicInfo();
        loadBidHistoryChart();
        loadBidHistoryCards();
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
        if (session == null || lblStatus == null) return;
        switch (session.getStatus()) {
            case OPEN -> lblStatus.setText("Coming");
            case RUNNING -> lblStatus.setText("Live");
            case FINISHED, PAID -> lblStatus.setText("Ended");
            case CANCELED -> lblStatus.setText("Canceled");
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

    private void loadBidHistoryChart() {
        if (lcPriceHistory == null || session == null) return;

        try {
            lcPriceHistory.getData().clear();
            List<Bid> bids = protocol.getBidHistory(session.getId());

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Price History");

            // Add opening starting price as baseline
            series.getData().add(new XYChart.Data<>("Start", session.getStartingPrice()));

            // Format formatting time or sequential index
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

    private void loadBidHistoryCards() {
        if (containerBidHistory == null || session == null) return;

        containerBidHistory.getChildren().clear();
        try {
            List<Bid> bids = protocol.getBidHistory(session.getId());
            // Sort bids newest first
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
            loadBidHistoryChart();
            loadBidHistoryCards();
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

        // Call backend
        String errorMsg = protocol.placeBidOrError(session.getId(), currentUser.getId(), bidAmount);
        if (errorMsg != null) {
            showAlert("Error", errorMsg);
            return;
        }

        // If autobid is selected, register it
        if (chboxAutobid.isSelected()) {
            try {
                double maxAmount = Double.parseDouble(txtMaxBidAmount.getText().trim());
                double increment = Double.parseDouble(txtIncrementAmount.getText().trim());
                if (maxAmount > bidAmount && increment > 0) {
                    AutoBidConfig config = new AutoBidConfig(0, currentUser.getId(), session.getId(), maxAmount, increment);
                    protocol.registerAutoBid(config);
                }
            } catch (NumberFormatException ignore) {}
        }

        // Refresh UI
        txtBidAmount.clear();
        refreshAuctionData();
        showAlert("Success", "Bid placed successfully!");
    }

    @FXML
    public void hanldeAutoBid(ActionEvent event) {
        boolean autoEnabled = chboxAutobid.isSelected();
        paneAutobid.setDisable(!autoEnabled);
        if (!autoEnabled) {
            txtMaxBidAmount.clear();
            txtIncrementAmount.clear();
            // Cancel auto bid on server
            User u = SessionContext.getCurrentUser();
            if (u != null && session != null) {
                protocol.cancelAutoBid(session.getId(), u.getId());
            }
        }
    }

    private void refreshAuctionData() {
        // Fetch latest session details
        List<AuctionSession> active = protocol.getActiveAuctions();
        for (AuctionSession s : active) {
            if (s.getId() == session.getId()) {
                this.session = s;
                bindBasicInfo();
                loadBidHistoryChart();
                loadBidHistoryCards();
                break;
            }
        }
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
