package com.auction.client.controller.Card;

import com.auction.shared.model.auction.Bid;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.time.format.DateTimeFormatter;

public class BidderHistoryCardController {

    @FXML private Label lblBidAmount;
    @FXML private Label lblBidDay;
    @FXML private Label lblBidTime;
    @FXML private Label lblBidder;

    public void setBid(Bid bid) {
        if (bid == null) return;

        if (lblBidAmount != null) {
            lblBidAmount.setText(String.format("$%,.2f", bid.getAmount()));
        }
        if (lblBidder != null && bid.getBidder() != null) {
            lblBidder.setText(bid.getBidder().getUsername());
        }
        if (bid.getTime() != null) {
            if (lblBidDay != null) {
                lblBidDay.setText(bid.getTime().format(DateTimeFormatter.ofPattern("dd/MM/yy")));
            }
            if (lblBidTime != null) {
                lblBidTime.setText(bid.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        }
    }
}
