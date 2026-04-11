package com.auction.shared.model.user;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bidder extends User {
    private double accountBalance;

    public Bidder(String id, String username, String password, String email, double accountBalance) {
        super(id, username, password, email);
        this.accountBalance = accountBalance;
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }

    public void placeBid(AuctionSession auctionSession, Bid bid){
        auctionSession.updateCurrentPrice(bid);
    }
}