package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.HistoryCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Màn hình My Bids.
 *
 * <p>Việc tải lịch sử bid có thể phát sinh nhiều request tới backend, nên màn hình này
 * gom chúng vào task nền rồi mới render UI để tránh làm đơ cửa sổ.</p>
 */
public class MyBidsController implements Initializable {
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML private Button btnTotalPane;
    @FXML private Button btnActivePane;
    @FXML private AnchorPane TotalPane;
    @FXML private AnchorPane ActivePane;
    @FXML private VBox containerTotal;
    @FXML private VBox containerActive;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadBidDataAsync();
        if (TotalPane != null) {
            TotalPane.toFront();
        }
    }

    @FXML
    private void switchHomePane(MouseEvent event) {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "Home");
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToSellerRole();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ITEMS, "items");
    }

    @FXML
    private void switchWalletPane(MouseEvent event) {
        SceneNavigator.loadScene(SceneNavigator.WALLET1, "wallet");
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) {
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }

    @FXML
    public void switchTab(ActionEvent event) {
        if (event.getSource() == btnTotalPane) {
            TotalPane.toFront();
            btnTotalPane.setStyle("-fx-background-color: #3a3386; -fx-text-fill: white; -fx-border-color: white; -fx-background-radius:20; -fx-border-radius: 20 ");
            btnActivePane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color: #3a3386; -fx-background-radius:20; -fx-border-radius: 20 ");
        } else if (event.getSource() == btnActivePane) {
            ActivePane.toFront();
            btnActivePane.setStyle("-fx-background-color: #3a3386; -fx-text-fill: white; -fx-border-color: white; -fx-background-radius:20; -fx-border-radius: 20 ");
            btnTotalPane.setStyle("-fx-background-color: white; -fx-text-fill: #3a3386; -fx-border-color: #3a3386; -fx-background-radius:20; -fx-border-radius: 20 ");
        }
    }

    private void loadBidDataAsync() {
        containerTotal.getChildren().clear();
        containerActive.getChildren().clear();
        if (SessionContext.getCurrentUser() == null) {
            showEmptyState(containerTotal, "You are not logged in.");
            showEmptyState(containerActive, "You are not logged in.");
            return;
        }

        showEmptyState(containerTotal, "Loading bid history...");
        showEmptyState(containerActive, "Loading active bids...");

        // Gom toàn bộ request lịch sử bid vào task nền để mở scene không bị đứng.
        FxAsync.run("my-bids-load", this::buildBidSnapshot,
                this::renderBidSnapshot,
                error -> {
                    showEmptyState(containerTotal, "Could not load bid history.");
                    showEmptyState(containerActive, "Could not load active bids.");
                });
    }

    private BidSnapshot buildBidSnapshot() {
        int myId = SessionContext.getCurrentUser().getId();
        List<Bid> myBidsHistory = new ArrayList<>(); // Tất cả lịch sử
        List<Bid> myActiveBids = new ArrayList<>();  // Các bid trong phiên đang chạy

        for (AuctionSession session : protocol.getActiveAuctions()) {
            List<Bid> sessionBids = protocol.getBidHistory(session.getId());

            Bid myLastBidInSession = null;
            for (Bid bid : sessionBids) {
                if (bid.getBidder() != null && bid.getBidder().getId() == myId) {
                    myBidsHistory.add(bid);
                    // Lưu lại bid cuối cùng của mình trong session này để hiện ở tab Active
                    if (myLastBidInSession == null || bid.getTime().isAfter(myLastBidInSession.getTime())) {
                        myLastBidInSession = bid;
                    }
                }
            }

            if (myLastBidInSession != null) {
                myActiveBids.add(myLastBidInSession);
            }
        }

        myBidsHistory.sort(Comparator.comparing(Bid::getTime).reversed());
        myActiveBids.sort(Comparator.comparing(Bid::getTime).reversed());

        return new BidSnapshot(myBidsHistory, myActiveBids);
    }

    private void renderBidSnapshot(BidSnapshot snapshot) {
        containerTotal.getChildren().clear();
        containerActive.getChildren().clear();

        // Render Tab Total History
        if (snapshot.bidHistory().isEmpty()) {
            showEmptyState(containerTotal, "No bid history yet.");
        } else {
            for (Bid bid : snapshot.bidHistory()) {
                loadHistoryCard(bid, containerTotal); // Thêm tham số container
            }
        }

        // Render Tab Active Bids
        if (snapshot.activeBids().isEmpty()) {
            showEmptyState(containerActive, "You do not have any active auctions with bids.");
        } else {
            for (Bid bid : snapshot.activeBids()) {
                loadHistoryCard(bid, containerActive); // Thêm tham số container
            }
        }
    }

    private void loadHistoryCard(Bid bid, VBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/HistoryCard.fxml"));
            Node card = loader.load();

            HistoryCardController controller = loader.getController();
            controller.setBid(bid);

            container.getChildren().add(card);
        } catch (IOException e) {
            Label fallback = new Label("Session #" + bid.getAuctionSession().getId() + " · " + String.format("%.0f $", bid.getAmount()));
            fallback.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-background-radius: 10;");
            container.getChildren().add(fallback);
        }
    }

    private void showEmptyState(VBox container, String message) {
        container.getChildren().clear();
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: #666666;");
        container.getChildren().add(label);
    }

    /**
     * Snapshot dữ liệu nhỏ dùng để chuyển kết quả đã tải từ worker thread
     * về lại JavaFX thread trong một object duy nhất.
     */
    // Sửa Record ở cuối file
    private record BidSnapshot(List<Bid> bidHistory, List<Bid> activeBids) {

    }
}
