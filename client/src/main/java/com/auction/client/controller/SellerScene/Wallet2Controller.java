package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.TransHisCardController;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.client.util.UserRoleSwitcher;
import com.auction.shared.model.user.Seller;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller cho màn Wallet (seller).
 *
 * <p>Hiển thị số dư tài khoản và lịch sử giao dịch.
 * Tất cả thao tác load dữ liệu chạy async để UI không bị đơ.</p>
 */
public class Wallet2Controller implements Initializable {

    @FXML private Label lblUsername;
    @FXML private Label lblTotalBalance;
    @FXML private Label lblAvailabe;
    @FXML private Label lblReserved;
    @FXML private VBox containerTrans;
    @FXML private HBox overlayPane;

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

        // Hiển thị trạng thái loading
        showLoadingState();

        // Fetch user info và transactions ở background
        FxAsync.run("seller-wallet-load",
                () -> {
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
     * Hiển thị trạng thái loading.
     */
    private void showLoadingState() {
        lblTotalBalance.setText("Loading...");
        lblAvailabe.setText("Loading...");
        lblReserved.setText("$0.00");
        containerTrans.getChildren().clear();
    }

    /**
     * Hiển thị trạng thái lỗi.
     */
    private void showErrorState() {
        lblTotalBalance.setText("Error");
        lblAvailabe.setText("Error");
        lblReserved.setText("$0.00");
    }

    /**
     * Render dữ liệu wallet sau khi load xong.
     */
    private void renderWalletData(WalletData data) {
        User u = data.user;
        System.out.println("Wallet updated! New Balance: " + ((Seller)u).getAccountBalance());

        SessionContext.setCurrentUser(u);
        SessionContext.setCurrentUser(u);

        double totalBalance = 0.0;
        double reservedBalance = 0.0; // Sellers không đặt bid nên không có reserved

        if (u instanceof Seller seller) {
            totalBalance = seller.getAccountBalance();
        }

        double availableBalance = totalBalance - reservedBalance;

        lblTotalBalance.setText("$" + String.format("%,.2f", totalBalance));
        lblAvailabe.setText("$" + String.format("%,.2f", availableBalance));
        lblReserved.setText("$" + String.format("%,.2f", reservedBalance));

        // Render transaction history
        containerTrans.getChildren().clear();
        for (Transaction trans : data.transactions) {
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
    public void switchBidderDB(MouseEvent mouseEvent) {
        UserRoleSwitcher.switchToBidderRole();
        SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder home");
    }

    @FXML
    public void switchSetting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SETTING2, "setting home");
    }

    @FXML
    public void switchHomePane(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller home");
    }

    @FXML
    public void switchMylisting(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "shipping home");
    }

    @FXML
    public void switchShipping(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.SHIPPING, "shipping home");
    }
}