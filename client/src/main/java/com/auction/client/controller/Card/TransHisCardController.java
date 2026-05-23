package com.auction.client.controller.Card;

import com.auction.shared.model.user.Transaction;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.time.format.DateTimeFormatter;

public class TransHisCardController {

    @FXML
    private Label lblTransTit;

    @FXML
    private Label lblTransDay;

    @FXML
    private Label lblTransTime;

    @FXML
    private Label lblTransAmount;

    public void setTransaction(Transaction trans) {
        String type = trans.getType();
        String desc = trans.getDescription();
        double amount = trans.getAmount();

        if (type.equals("DEPOSIT")) {
            lblTransTit.setText("Deposit");
            lblTransAmount.setText("+$" + String.format("%,.2f", amount));
            lblTransAmount.setStyle("-fx-text-fill: green;");
        } else if (type.equals("WITHDRAW")) {
            lblTransTit.setText("Withdraw");
            lblTransAmount.setText("-$" + String.format("%,.2f", amount));
            lblTransAmount.setStyle("-fx-text-fill: red;");
        } else if (type.equals("BID_SUCCESS")) {
            lblTransTit.setText("Successfully bid for " + desc);
            boolean isBidder = com.auction.client.SessionContext.getCurrentUser() instanceof com.auction.shared.model.user.Bidder;
            if (isBidder) {
                lblTransAmount.setText("-$" + String.format("%,.2f", amount));
                lblTransAmount.setStyle("-fx-text-fill: red;");
            } else {
                lblTransAmount.setText("+$" + String.format("%,.2f", amount));
                lblTransAmount.setStyle("-fx-text-fill: green;");
            }
        }

        if (trans.getTime() != null) {
            lblTransDay.setText(trans.getTime().format(DateTimeFormatter.ofPattern("dd/MM/yy")));
            lblTransTime.setText(trans.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
}
