package com.auction.client.controller.Card;

import com.auction.shared.model.auction.AuctionSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class AuctionResult1CardController {

    @FXML private Label lblWinner;
    @FXML private Label lblWinningPrice;
    @FXML private HBox btnCfShipping;

    private AuctionSession session;

    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        if (session == null) return;

        if (lblWinner != null && session.getWinner() != null) {
            lblWinner.setText(session.getWinner().getUsername());
        }
        if (lblWinningPrice != null) {
            lblWinningPrice.setText(String.format("$%,.2f", session.getCurrentPrice()));
        }
    }

    @FXML
    public void handleShipped(MouseEvent event) {
        if (session == null) return;

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Shipping Confirmed");
            alert.setHeaderText(null);
            alert.setContentText("Shipping confirmed for order " + (session.getItem() != null ? session.getItem().getName() : "") + "!");
            alert.showAndWait();

            if (btnCfShipping != null) {
                btnCfShipping.setStyle("-fx-background-color: #9e9e9e; -fx-background-radius: 15;");
                btnCfShipping.setDisable(true);
            }
        });
    }
}
