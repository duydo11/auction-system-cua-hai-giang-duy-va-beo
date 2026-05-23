package com.auction.client.controller.Card;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class OrderShippingCardController {

    @FXML private Label lblItemName;
    @FXML private Label lblWinner;
    @FXML private Label lblWinningPrice;
    @FXML private Button btnViewDetails;

    private AuctionSession session;

    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        if (session == null) return;

        if (lblItemName != null && session.getItem() != null) {
            lblItemName.setText(session.getItem().getName());
        }
        if (lblWinner != null && session.getWinner() != null) {
            lblWinner.setText(session.getWinner().getUsername());
        }
        if (lblWinningPrice != null) {
            lblWinningPrice.setText(String.format("$%,.2f", session.getCurrentPrice()));
        }
    }

    @FXML
    public void handleViewDetails(ActionEvent event) {
        if (session == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionsScene/AuctionDetailsforSeller.fxml"));
            Parent root = loader.load();

            com.auction.client.controller.ActionsScene.AuctionDetailsforSellerController controller = loader.getController();
            controller.setAuctionSession(session);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Chi tiết phiên bán: " + (session.getItem() != null ? session.getItem().getName() : ""));
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (IOException e) {
            System.err.println("Error loading details dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
