package com.auction.shared.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private int bidId;
    private int auctionId;
    private int bidderId;
    private double bidAmount;
    private String bidTime;

    public Bid(int auctionId, int bidderId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }
}