package com.auction.client.controller.ActionsScene;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;

public class WithdrawActionController {

    @FXML
    private TextField txtWithdrawAmount;

    @FXML
    private HBox btnConfirm;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    public void handleConfirm(MouseEvent event) {
        String amountText = txtWithdrawAmount.getText();
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
                double totalBalance = 0.0;
                double reservedBalance = 0.0;

                // Sync latest profile from server
                User latestUser = protocol.getUserInfo(currentUser.getId());
                if (latestUser != null) {
                    currentUser = latestUser;
                    SessionContext.setCurrentUser(latestUser);
                }

                if (currentUser instanceof Bidder) {
                    Bidder bidder = (Bidder) currentUser;
                    totalBalance = bidder.getAccountBalance();

                    // Calculate reserved balance
                    List<AuctionSession> activeAuctions = protocol.getActiveAuctions();
                    for (AuctionSession session : activeAuctions) {
                        if (session.getWinner() != null && session.getWinner().getId() == bidder.getId()) {
                            reservedBalance += session.getCurrentPrice();
                        }
                    }
                } else if (currentUser instanceof Seller) {
                    Seller seller = (Seller) currentUser;
                    totalBalance = seller.getAccountBalance();
                }

                double availableBalance = totalBalance - reservedBalance;

                if (amount > availableBalance) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Rút tiền thất bại");
                    alert.setHeaderText(null);
                    alert.setContentText("Số tiền muốn rút vượt quá số dư khả dụng!");
                    alert.showAndWait();
                    return;
                }

                if (currentUser instanceof Bidder) {
                    Bidder bidder = (Bidder) currentUser;
                    bidder.setAccountBalance(bidder.getAccountBalance() - amount);
                } else if (currentUser instanceof Seller) {
                    Seller seller = (Seller) currentUser;
                    seller.setAccountBalance(seller.getAccountBalance() - amount);
                }
                boolean success = protocol.withdraw(currentUser, amount);
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
