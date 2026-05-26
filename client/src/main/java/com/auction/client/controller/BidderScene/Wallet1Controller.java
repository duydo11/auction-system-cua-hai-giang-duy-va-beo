package com.auction.client.controller.BidderScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.TransHisCardController;
import com.auction.client.network.ClientProtocolHandler;
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
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

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

        loadWalletData();
    }

    private void loadWalletData() {
        var u = SessionContext.getCurrentUser();
        if (u == null) return;

        // Fetch latest user details from server to sync balance
        User latest = protocol.getUserInfo(u.getId());
        if (latest != null) {
            u = latest;
            SessionContext.setCurrentUser(latest);
        }

        double totalBalance = 0.0;
        double reservedBalance = 0.0;

        if (u instanceof Bidder) {
            Bidder bidder = (Bidder) u;
            totalBalance = bidder.getAccountBalance();

            // Calculate reserved balance: leading bids in active sessions
            List<AuctionSession> activeAuctions = protocol.getActiveAuctions();
            for (AuctionSession session : activeAuctions) {
                if (session.getWinner() != null && session.getWinner().getId() == bidder.getId()) {
                    reservedBalance += session.getCurrentPrice();
                }
            }
        }

        double availableBalance = totalBalance - reservedBalance;

        lblTotalBalance.setText("$" + String.format("%,.2f", totalBalance));
        lblAvailabe.setText("$" + String.format("%,.2f", availableBalance));
        lblReserved.setText("$" + String.format("%,.2f", reservedBalance));

        // Load transaction history
        containerTrans.getChildren().clear();
        List<Transaction> transactions = protocol.getTransactions(u.getId());
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

    @FXML
    public void handleDeposit(MouseEvent mouseEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/DepositAction.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Deposit Funds");

            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            overlayPane.setVisible(true);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            overlayPane.setVisible(false);

            // Reload wallet data
            loadWalletData();

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

            // Reload wallet data
            loadWalletData();

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
            System.err.println("Lỗi: Không tìm thấy file");
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