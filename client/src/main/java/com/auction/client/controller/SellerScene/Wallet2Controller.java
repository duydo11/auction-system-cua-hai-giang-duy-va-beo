package com.auction.client.controller.SellerScene;

import com.auction.client.SessionContext;
import com.auction.client.controller.Card.TransHisCardController;
import com.auction.client.network.ClientProtocolHandler;
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
        double reservedBalance = 0.0; // Sellers do not place bids

        if (u instanceof Seller) {
            Seller seller = (Seller) u;
            totalBalance = seller.getAccountBalance();
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