package com.auction.client.controller.Card;

import com.auction.shared.model.auction.Bid;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

/**
 * Controller cho HistoryCard — bind data từ Bid.
 * Giang cần thêm fx:id vào FXML:
 * - lblBidAmount (giá đặt)
 * - lblBidTime (thời gian)
 * - lblBidder (tên người đặt)
 * - lblAuctionId (mã phiên)
 */
public class HistoryCardController {
    
    @FXML private Label lblBidAmount;
    @FXML private Label lblBidTime;
    @FXML private Label lblBidder;
    @FXML private Label lblAuctionId;
    
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    
    /**
     * Bind data từ Bid vào card.
     */
    public void setBid(Bid bid) {
        if (bid == null) {
            return;
        }
        
        if (lblBidAmount != null) {
            lblBidAmount.setText(String.format("%.0f VND", bid.getAmount()));
        }
        
        if (lblBidTime != null && bid.getTime() != null) {
            lblBidTime.setText(bid.getTime().format(TIME_FMT));
        }
        
        if (lblBidder != null && bid.getBidder() != null) {
            lblBidder.setText(bid.getBidder().getUsername());
        }
        
        if (lblAuctionId != null && bid.getAuctionSession() != null) {
            lblAuctionId.setText("Phiên #" + bid.getAuctionSession().getId());
        }
    }
}
