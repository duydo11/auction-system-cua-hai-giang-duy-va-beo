package com.auction.server;

import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;

/**
 * Một bộ service dùng chung cho mọi {@link com.auction.server.network.ClientHandler}
 * (đồng bộ logic đấu giá / giảm overhead).
 */
public final class ServiceRegistry {

    public static final UserService USER_SERVICE = new UserService();
    public static final AuctionService AUCTION_SERVICE = new AuctionService();
    public static final BidService BID_SERVICE = new BidService();
    public static final AutoBidService AUTO_BID_SERVICE = new AutoBidService();

    private ServiceRegistry() {
    }
}

