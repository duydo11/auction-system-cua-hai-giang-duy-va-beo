package com.auction.shared.model.auction;

import com.auction.shared.model.Entity;
import com.auction.shared.model.user.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class Bid extends Entity {
    private User bidder;
    private double amount;
    private LocalDateTime time;
    private AuctionSession auctionSession;

    public Bid(String id, User bidder,AuctionSession auctionSession , double amount){
        super(id);
        this.bidder = bidder;
        this.amount = amount;
        this.time = LocalDateTime.now();
        this.auctionSession = auctionSession;
    }


}
