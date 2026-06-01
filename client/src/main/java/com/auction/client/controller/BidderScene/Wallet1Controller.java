package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.TransHisCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.User;
import com.auction.shared.model.user.Transaction;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Controller cho màn Wallet (bidder).
 *
 * <p>Hiển thị số dư tài khoản, reserved balance và lịch sử giao dịch.
 * Tất cả thao tác load dữ liệu chạy async để UI không bị đơ.</p>
 */
public class Wallet1Controller implements Initializable {

    @FXML private HBox overlayPane;
    @FXML private Label lblUsername;
    @FXML private Label lblTotalBalance;
    @FXML private Label lblAvailabe;
    @FXML private Label lblReserved;
    @FXML private VBox containerTrans;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");

        // Load wallet data async
        loadWalletDataAsync();
    }

    /**
     * Load dữ liệu wallet ở background thread.
     */
    private void loadWalletDataAsync() {
        var u = SessionContext.getCurrentUser();
        if (u == null) return;

        // Hiển thị ngay số dư từ SessionContext để không phải chờ network.
        renderInstantBalance(u);

        // Chỉ fetch transactions ở background (user info và active auctions dùng từ cache/session).
        FxAsync.run("wallet-load",
                () -> {
                    // Lấy user mới nhất để cập nhật balance nếu có thay đổi.
                    User latest = protocol.getUserInfo(u.getId());
                    if (latest == null) latest = u;
                    List<Transaction> transactions = protocol.getTransactions(latest.getId());
                    return new WalletData(latest, transactions);
                },
                this::renderWalletData,
                error -> {
                    System.err.println("Error loading wallet data: " + error);
                    showErrorState();
                });
    }

    /**
     * Render balance ngay từ session context, không cần chờ network.
     */
    private void renderInstantBalance(User u) {
        if (u instanceof Bidder bidder) {
            double totalBalance = bidder.getAccountBalance();
            lblTotalBalance.setText("$" + String.format("%,.2f", totalBalance));
            lblAvailabe.setText("$" + String.format("%,.2f", totalBalance));
            lblReserved.setText("$0.00");
        }
    }

    /**
     * Hiển thị trạng thái loading.
     */
    private void showLoadingState() {
        lblTotalBalance.setText("Loading...");
        lblAvailabe.setText("Loading...");
        lblReserved.setText("Loading...");
        containerTrans.getChildren().clear();
    }

    /**
     * Hiển thị trạng thái lỗi.
     */
    private void showErrorState() {
        lblTotalBalance.setText("Error");
        lblAvailabe.setText("Error");
        lblReserved.setText("Error");
    }

    /**
     * Render dữ liệu wallet sau khi load xong.
     */
    private void renderWalletData(WalletData data) {
        User u = data.user();

        SessionContext.setCurrentUser(u);

        double totalBalance = 0.0;
        double reservedBalance = 0.0;

        if (u instanceof Bidder bidder) {
            totalBalance = bidder.getAccountBalance();

            // Tính reserved balance từ active cache để tránh thêm 1 network call.
            for (AuctionSession session : com.auction.client.util.AuctionCache.getActive()) {
                if (session.getWinner() != null && session.getWinner().getId() == bidder.getId()) {
                    reservedBalance += session.getCurrentPrice();
                }
            }
        }

        double availableBalance = totalBalance - reservedBalance;

        lblTotalBalance.setText("$" + String.format("%,.2f", totalBalance));
        lblAvailabe.setText("$" + String.format("%,.2f", availableBalance));
        lblReserved.setText("$" + String.format("%,.2f", reservedBalance));

        // Render transaction history
        containerTrans.getChildren().clear();
        List<Transaction> transactions = deduplicateSettlementTransactions(data.transactions());
        for (Transaction trans : transactions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Card/TransHisCard.fxml"));
                Node card = loader.load();
                TransHisCardController cardCtrl = loader.getController();
                cardCtrl.setTransaction(trans);
                containerTrans.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Error loading TransHisCard: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private List<Transaction> deduplicateSettlementTransactions(List<Transaction> transactions) {
        Set<String> seen = new HashSet<>();
        return transactions.stream()
                .filter(trans -> {
                    String type = trans.getType();
                    if (!"BID_PAYMENT".equals(type) && !"BID_SUCCESS".equals(type) && !"AUCTION_SALE".equals(type)) {
                        return true;
                    }
                    String desc = trans.getDescription() == null ? "" : trans.getDescription();
                    if (desc.startsWith("#")) {
                        int firstSpace = desc.indexOf(' ');
                        if (firstSpace > 0) {
                            desc = desc.substring(0, firstSpace);
                        }
                    }
                    String key = type + "|" + desc + "|" + String.format("%.2f", trans.getAmount());
                    return seen.add(key);
                })
                .toList();
    }

    /**
     * Data class để truyền dữ liệu từ background thread.
     */
    private record WalletData(User user, List<Transaction> transactions) {}

    @FXML
    public void handleDeposit(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/DepositAction.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Deposit Funds");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            overlayPane.setVisible(true);
            dialogStage.setScene(new Scene(root));

            // Đợi cửa sổ đóng
            dialogStage.showAndWait();

            overlayPane.setVisible(false);

            // QUAN TRỌNG: Xóa cache thủ công nếu có hoặc gọi trực tiếp từ protocol
            System.out.println("Reloading wallet after deposit...");
            loadWalletDataAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleWithdraw(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/WithdrawAction.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Withdraw Funds");

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            overlayPane.setVisible(true);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            overlayPane.setVisible(false);

            // Reload wallet data async
            loadWalletDataAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== Navigation ====================

    @FXML
    private void switchHomePane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "home");
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) throws IOException {
        UserRoleSwitcher.switchToSellerRole();
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
    }

    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/Items.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Error: File not found");
            e.printStackTrace();
        }
    }

    @FXML
    public void switchMybidsPane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_BIDS, "my bids");
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.SETTING1, "Setting");
    }
}