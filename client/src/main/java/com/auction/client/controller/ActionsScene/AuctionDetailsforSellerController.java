package com.auction.client.controller.ActionsScene;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.controller.Card.AuctionResult1CardController;
import com.auction.client.controller.Card.AuctionResult2CardController;
import com.auction.client.controller.Card.BidderHistoryCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.auction.Bid;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
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
 * Controller for the seller auction detail screen.
 *
 * <p>The seller sees the same live auction data as bidders but from the owner perspective:</p>
 * <ul>
 *   <li>binds product, seller, price, bid count and auction status,</li>
 *   <li>renders bid history and the price history chart,</li>
 *   <li>listens for realtime server pushes to keep the view synchronized,</li>
 *   <li>shows anti-sniping/ending-soon messages,</li>
 *   <li>loads the final result card when the auction is finished.</li>
 * </ul>
 */
public class AuctionDetailsforSellerController implements Initializable {

    @FXML private Label lblItemName;
    @FXML private Label lblItemDetails;
    @FXML private Label lblSeller;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartPrice;
    @FXML private Label lblTimeH;
    @FXML private Label lblTimem;
    @FXML private Label lblTimes;
    @FXML private Label lblCurrentBids;
    @FXML private LineChart<String, Number> lcPriceHistory;
    @FXML private VBox containerBidHistory;
    @FXML private HBox containerResult;
    @FXML private Label lblStatus;
    @FXML private Label lblWarning;

    private AuctionSession session;
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private Timeline countdownTimer;
    private Consumer<AuctionSession> realtimeListener;
    private LocalDateTime lastKnownEndTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        if (session == null) return;

        this.lastKnownEndTime = session.getEndTime();
        bindBasicInfo();
        loadBidHistoryChart();
        loadBidHistoryCards();
        loadAuctionResult();
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
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(String.format("$%,.2f", session.getCurrentPrice()));
        }
        if (lblStartPrice != null) {
            lblStartPrice.setText(String.format("$%,.2f", session.getStartingPrice()));
        }
        if (lblCurrentBids != null) {
            lblCurrentBids.setText(String.valueOf(session.getBids().size()));
        }
        if (lblStatus != null && session.getStatus() != null) {
            switch (session.getStatus()) {
                case OPEN -> lblStatus.setText("Coming");
                case RUNNING -> lblStatus.setText("Live");
                case FINISHED, PAID -> lblStatus.setText("Ended");
                case CANCELED -> lblStatus.setText("Canceled");
            }
        }
    }

    private void loadBidHistoryChart() {
        if (lcPriceHistory == null || session == null) return;

        try {
            lcPriceHistory.getData().clear();
            List<Bid> bids = protocol.getBidHistory(session.getId());

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Price History");

            series.getData().add(new XYChart.Data<>("Start", session.getStartingPrice()));

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

    private void loadAuctionResult() {
        if (containerResult == null || session == null) return;

        containerResult.getChildren().clear();

        // Only load result card if session is ended (FINISHED or PAID or CANCELED)
        if (session.getStatus() == AuctionStatus.OPEN || session.getStatus() == AuctionStatus.RUNNING) {
            containerResult.setVisible(false);
            return;
        }

        containerResult.setVisible(true);
        try {
            if (session.getWinner() != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/AuctionResult1Card.fxml"));
                Node card = loader.load();
                AuctionResult1CardController ctrl = loader.getController();
                ctrl.setAuctionSession(session);
                containerResult.getChildren().add(card);
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/AuctionResult2Card.fxml"));
                Node card = loader.load();
                AuctionResult2CardController ctrl = loader.getController();
                ctrl.setAuctionSession(session);
                containerResult.getChildren().add(card);
            }
        } catch (IOException e) {
            System.err.println("Error loading AuctionResultCard: " + e.getMessage());
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
                lblWarning.setText("Auction ended. Result card will appear when server closes the session.");
                lblWarning.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold;");
            }
            if (countdownTimer != null) countdownTimer.stop();

            // Reload results when countdown ends
            loadAuctionResult();
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
            lblWarning.setText("⚠️ Last 30 seconds: bidder activity may extend this auction.");
            lblWarning.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
        } else if (secondsRemaining <= 300) {
            lblWarning.setText("Auction ending soon. Watch realtime bid updates.");
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
            loadAuctionResult();
            updateTimeLabels();
            if (lblWarning != null && previousEnd != null && updatedSession.getEndTime() != null
                    && updatedSession.getEndTime().isAfter(previousEnd)) {
                lblWarning.setText("Anti-sniping activated: auction extended by server.");
                lblWarning.setStyle("-fx-text-fill: #047857; -fx-font-weight: bold;");
            }
        };
        RealtimeAuctionBus.addAuctionListener(realtimeListener);
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
