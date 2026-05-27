package com.auction.client.controller.ActionsScene;

import com.auction.client.RealtimeAuctionBus;
import com.auction.client.controller.Card.AuctionResult1CardController;
import com.auction.client.controller.Card.AuctionResult2CardController;
import com.auction.client.controller.Card.BidderHistoryCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
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
 * Controller cho màn chi tiết auction (seller view).
 *
 * <p>Seller thấy dữ liệu auction realtime giống bidder nhưng từ góc nhìn chủ sở hữu:</p>
 * <ul>
 *   <li>Bind thông tin sản phẩm, seller, giá, số bid và trạng thái auction</li>
 *   <li>Render bid history và price history chart</li>
 *   <li>Lắng nghe realtime server pushes để giữ view đồng bộ</li>
 *   <li>Hiển thị cảnh báo anti-sniping/ending-soon</li>
 *   <li>Load result card khi auction kết thúc</li>
 * </ul>
 *
 * <p><strong>Async strategy:</strong> Tất cả thao tác network (load bid history)
 * chạy ở background thread để UI không bị đơ.</p>
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
        
        // Load bid history async để không block UI khi mở dialog
        loadBidHistoryAsync();
        
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

    /**
     * Load bid history ở background thread để tránh đơ UI khi mở dialog.
     */
    private void loadBidHistoryAsync() {
        if (session == null) return;
        
        // Hiển thị trạng thái loading
        showLoadingState();
        
        // Fetch bid history ở background
        FxAsync.run("seller-bid-history-" + session.getId(),
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

    /**
     * Render bid history cards từ danh sách bid đã load.
     */
    private void renderBidHistoryCards(List<Bid> bids) {
        if (containerBidHistory == null) return;

        containerBidHistory.getChildren().clear();
        try {
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

        // Chỉ load result card nếu session đã kết thúc (FINISHED hoặc PAID hoặc CANCELED)
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

            // Reload results khi countdown kết thúc
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
            loadBidHistoryAsync();
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
