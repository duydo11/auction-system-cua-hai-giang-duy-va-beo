package com.auction.client.controller.ActionsScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller cho dialog Withdraw.
 *
 * <p>Xử lý rút tiền từ tài khoản. Thao tác withdraw chạy async
 * để UI không bị đơ khi chờ server phản hồi.</p>
 */
public class WithdrawActionController {

    @FXML private TextField txtWithdrawAmount;
    @FXML private HBox btnConfirm;
    @FXML private Label lblMessage;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    public void handleConfirm(MouseEvent event) {
        String amountText = txtWithdrawAmount.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            showMessage("Please enter an amount.", true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
            if (amount <= 0) {
                showMessage("Amount must be greater than 0.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Invalid number format.", true);
            return;
        }

        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) {
            showMessage("Not logged in.", true);
            return;
        }

        // Disable button để tránh double-click
        btnConfirm.setDisable(true);
        showMessage("Checking balance...", false);

        final double withdrawAmount = amount;

        // Fetch user info và active auctions ở background để tính available balance
        FxAsync.run("withdraw-check",
                () -> {
                    User latestUser = protocol.getUserInfo(currentUser.getId());
                    if (latestUser == null) latestUser = currentUser;
                    
                    List<AuctionSession> activeAuctions = protocol.getActiveAuctions();
                    
                    return new WithdrawData(latestUser, activeAuctions);
                },
                data -> processWithdraw(data, withdrawAmount),
                error -> {
                    showMessage("Error checking balance: " + error, true);
                    btnConfirm.setDisable(false);
                });
    }

    /**
     * Xử lý withdraw sau khi đã check balance.
     */
    private void processWithdraw(WithdrawData data, double amount) {
        User currentUser = data.user;
        SessionContext.setCurrentUser(currentUser);

        double totalBalance = 0.0;
        double reservedBalance = 0.0;

        if (currentUser instanceof Bidder bidder) {
            totalBalance = bidder.getAccountBalance();

            // Tính reserved balance: các bid đang dẫn đầu
            for (AuctionSession session : data.activeAuctions) {
                if (session.getWinner() != null && session.getWinner().getId() == bidder.getId()) {
                    reservedBalance += session.getCurrentPrice();
                }
            }
        } else if (currentUser instanceof Seller seller) {
            totalBalance = seller.getAccountBalance();
        }

        double availableBalance = totalBalance - reservedBalance;

        if (amount > availableBalance) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Withdrawal Failed");
            alert.setHeaderText(null);
            alert.setContentText("Withdrawal amount exceeds available balance!");
            alert.showAndWait();
            btnConfirm.setDisable(false);
            return;
        }

        // Thực hiện withdraw
        showMessage("Processing withdrawal...", false);

        FxAsync.run("withdraw",
                () -> {
                    // Update local balance
                    if (currentUser instanceof Bidder bidder) {
                        bidder.setAccountBalance(bidder.getAccountBalance() - amount);
                    } else if (currentUser instanceof Seller seller) {
                        seller.setAccountBalance(seller.getAccountBalance() - amount);
                    }
                    
                    return protocol.withdraw(currentUser, amount);
                },
                success -> {
                    if (success) {
                        // Close the dialog stage
                        Stage stage = (Stage) btnConfirm.getScene().getWindow();
                        stage.close();
                    } else {
                        showMessage("Withdrawal failed. Please try again.", true);
                        btnConfirm.setDisable(false);
                    }
                },
                error -> {
                    showMessage("Error: " + error, true);
                    btnConfirm.setDisable(false);
                });
    }

    /**
     * Data class để truyền dữ liệu từ background thread.
     */
    private record WithdrawData(User user, List<AuctionSession> activeAuctions) {}

    /**
     * Hiển thị message trong dialog.
     */
    private void showMessage(String text, boolean isError) {
        if (lblMessage != null) {
            lblMessage.setText(text);
            lblMessage.setStyle(isError ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #1976d2;");
        }
    }
}
