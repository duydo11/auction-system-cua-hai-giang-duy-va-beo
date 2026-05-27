package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.AuctionCache;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Màn hình My Listing cho seller.
 *
 * <p>Phần truy vấn danh sách sản phẩm được chạy ở task nền để khi mở màn này
 * cửa sổ JavaFX không bị đứng do chờ backend.</p>
 */
public class MyListingController {

    @FXML private HBox overlayPane;
    @FXML private VBox containerList;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    public void initialize() {
        loadSellerListingsAsync();
    }

    private void loadSellerListingsAsync() {
        var current = SessionContext.getCurrentUser();
        if (current == null) {
            containerList.getChildren().clear();
            showMessage("You are not logged in.");
            return;
        }

        if (AuctionCache.hasAllData()) {
            renderSellerListings(filterSellerAuctions(AuctionCache.getAll(), current.getId()));
        } else {
            containerList.getChildren().clear();
            showMessage("Loading your listings...");
        }

        // My Listings phải luôn gọi getAllAuctions để lấy đủ lịch sử seller.
        // Không dùng active cache của bidder vì active cache chỉ là các phiên đang đặt giá được.
        FxAsync.run("my-listings-load", protocol::getAllAuctions,
                auctions -> renderSellerListings(filterSellerAuctions(auctions, current.getId())),
                error -> {
                    if (!AuctionCache.hasAllData()) {
                        showMessage("Could not load product cards.");
                    }
                });
    }

    private void renderSellerListings(List<AuctionSession> auctions) {
        containerList.getChildren().clear();
        if (auctions.isEmpty()) {
            showMessage("You don't have any product");
            return;
        }

        for (AuctionSession session : auctions) {
            containerList.getChildren().add(createSellerHistoryRow(session));
        }
    }

    private HBox createSellerHistoryRow(AuctionSession session) {
        String itemName = session.getItem() != null ? session.getItem().getName() : "Unknown product";
        String buyer = session.getWinner() != null ? session.getWinner().getUsername() : "No Bidders Yet";
        String start = formatTime(session.getStartTime());
        String end = formatTime(session.getEndTime());

        Label name = new Label(itemName);
        name.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2f5f90;");
        Label buyerLabel = new Label("Bidder: " + buyer);
        Label price = new Label(String.format("Current price: %,.0f $", session.getCurrentPrice()));
        Label time = new Label("Start: " + start + "  •  End: " + end);
        Label status = new Label(session.getStatus() != null ? session.getStatus().name() : "OPEN");
        status.setStyle("-fx-background-color: #eaf4ff; -fx-text-fill: #438bcf; -fx-background-radius: 12; -fx-padding: 4 10 4 10;");

        VBox info = new VBox(4, name, buyerLabel, price, time);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, info, spacer, status);
        row.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d7e7f7; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14;");
        row.setPrefWidth(900);
        row.setOnMouseClicked(event -> {
            try {
                // 1. Load file FXML của trang chi tiết người bán
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ActionsScene/AuctionDetailsforSeller.fxml"));
                javafx.scene.Parent root = loader.load();

                // 2. Truyền dữ liệu session sang Controller của trang chi tiết
                com.auction.client.controller.ActionsScene.AuctionDetailsforSellerController controller = loader.getController();
                controller.setAuctionSession(session);

                // 3. Tạo một cửa sổ mới (Stage) để hiển thị
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Auction Management: " + (session.getItem() != null ? session.getItem().getName() : "Details"));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Hiện dạng Pop-up ghi đè
                stage.setScene(new javafx.scene.Scene(root));

                // Dọn dẹp listener khi đóng cửa sổ
                stage.setOnCloseRequest(e -> controller.cleanup());

                stage.show();
            } catch (java.io.IOException e) {
                System.err.println("Error opening Auction Details for Seller: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Thêm hiệu ứng đổi màu chuột khi di chuyển qua hàng để người dùng biết có thể ấn được
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle() + "-fx-background-color: #f0f7ff; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle() + "-fx-background-color: #ffffff;"));

        return row;
    }

    private List<AuctionSession> filterSellerAuctions(List<AuctionSession> auctions, int sellerId) {
        return auctions.stream()
                .filter(s -> s.getSeller() != null && s.getSeller().getId() == sellerId)
                .toList();
    }

    private String formatTime(java.time.LocalDateTime time) {
        if (time == null) {
            return "N/A";
        }
        return time.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private void showMessage(String message) {
        containerList.getChildren().clear();
        Label label = new Label(message);
        label.setStyle("-fx-font-family: 'Montserrat'; -fx-font-size: 14; -fx-text-fill: #666666;");
        containerList.getChildren().add(label);
    }

    @FXML
    public void switchBidderDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToBidderRole();
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }

    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }

    public void switchHomePane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller home");
    }

    public void switchWallet(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.WALLET2, "wallet home");
    }

    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }

    public void handleAddproduct(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/AddProductDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create new listing");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            overlayPane.setVisible(true);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            overlayPane.setVisible(false);
            loadSellerListingsAsync();
        } catch (IOException e) {
            overlayPane.setVisible(false);
            e.printStackTrace();
        }
    }
}
