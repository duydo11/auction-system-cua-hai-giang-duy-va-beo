package com.auction.shared.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int auctionId;
    private String title;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private int sellerId;
    private String status; // ACTIVE, ENDED, CANCELLED
    private String startDate;
    private String endDate;
    private String category;
    private int totalBids;

    public Auction(String title, String description, double startingPrice,
                   int sellerId, String category) {
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.sellerId = sellerId;
        this.category = category;
        this.status = "ACTIVE";
        this.totalBids = 0;
    }
}