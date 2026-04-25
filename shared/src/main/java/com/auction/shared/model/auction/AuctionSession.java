package com.auction.shared.model.auction;

import com.auction.shared.model.Entity;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AuctionSession extends Entity {
    private User seller, winner;
    private Item item;
    private double startingPrice, currentPrice;
    private LocalDateTime startTime, endTime;
    private List<Bid> bids;

    public AuctionSession(String id,
                          User seller,
                          Item item, double startingPrice,
                          LocalDateTime startTime,
                          LocalDateTime endTime) {
        super(id);
        this.seller = seller;
        this.winner = null;
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bids = new ArrayList<>();
    }

    public void updateCurrentPrice(Bid bid) {
        if (this.isActive()) {
            if (bid.getAmount() > this.currentPrice
                    && this.startTime.isBefore(bid.getTime())
                    && this.endTime.isAfter(bid.getTime())) {
                this.currentPrice = bid.getAmount();
                this.winner = bid.getBidder();
                bids.add(bid);
                System.out.println("Updated price successfully, new price is: " + bid.getAmount());
            } else {
                System.out.println("Invalid bid");
            }
        } else {
            System.out.println("Time runs out");
        }
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

}

