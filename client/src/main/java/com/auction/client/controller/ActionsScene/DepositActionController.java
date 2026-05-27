package com.auction.client.controller.ActionsScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller cho dialog Deposit.
 *
 * <p>Xử lý nạp tiền vào tài khoản. Thao tác deposit chạy async
 * để UI không bị đơ khi chờ server phản hồi.</p>
 */
public class DepositActionController {

    @FXML private TextField txtDepositAmount;
    @FXML private HBox btnConfirm;
    @FXML private Label lblMessage;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    public void handleConfirm(MouseEvent event) {
        String amountText = txtDepositAmount.getText();
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
        showMessage("Processing deposit...", false);

        // Gọi deposit ở background thread
        FxAsync.run("deposit",
                () -> {
                    // Update local balance
                    if (currentUser instanceof Bidder bidder) {
                        bidder.setAccountBalance(bidder.getAccountBalance() + amount);
                    } else if (currentUser instanceof Seller seller) {
                        seller.setAccountBalance(seller.getAccountBalance() + amount);
                    }
                    
                    return protocol.deposit(currentUser, amount);
                },
                success -> {
                    if (success) {
                        // Close the dialog stage
                        Stage stage = (Stage) btnConfirm.getScene().getWindow();
                        stage.close();
                    } else {
                        showMessage("Deposit failed. Please try again.", true);
                        btnConfirm.setDisable(false);
                    }
                },
                error -> {
                    showMessage("Error: " + error, true);
                    btnConfirm.setDisable(false);
                });
    }

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
