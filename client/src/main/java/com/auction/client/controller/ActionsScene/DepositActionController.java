package com.auction.client.controller.ActionsScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class DepositActionController {

    @FXML
    private TextField txtDepositAmount;

    @FXML
    private HBox btnConfirm;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    public void handleConfirm(MouseEvent event) {
        String amountText = txtDepositAmount.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            return;
        }

        try {
            double amount = Double.parseDouble(amountText.trim());
            if (amount <= 0) {
                return;
            }

            User currentUser = SessionContext.getCurrentUser();
            if (currentUser != null) {
                if (currentUser instanceof Bidder) {
                    Bidder bidder = (Bidder) currentUser;
                    bidder.setAccountBalance(bidder.getAccountBalance() + amount);
                } else if (currentUser instanceof Seller) {
                    Seller seller = (Seller) currentUser;
                    seller.setAccountBalance(seller.getAccountBalance() + amount);
                }
                boolean success = protocol.deposit(currentUser, amount);
                if (!success) {
                    return;
                }
            }

            // Close the dialog stage
            Stage stage = (Stage) btnConfirm.getScene().getWindow();
            stage.close();

        } catch (NumberFormatException e) {
            // Invalid number format, do nothing
        }
    }
}
