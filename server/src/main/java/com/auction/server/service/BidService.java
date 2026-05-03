package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;

import java.util.Collections;
import java.util.List;

public class BidService {
    private final AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO();
    private final BidDAO bidDAO = new BidDAO();
    private final UserDAO userDAO = new UserDAO();

    public boolean placeBid(int sessionId, int bidderId, double amount) {
        AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
        User bidder = userDAO.getUserById(bidderId);
        if (session == null || bidder == null) {
            return false;
        }
        int bidsBefore = session.getBids().size();
        int bidId = bidDAO.allocateNextBidId();
        Bid bid = new Bid(bidId, bidder, session, amount);
        session.updateCurrentPrice(bid);
        if (session.getBids().size() <= bidsBefore) {
            return false;
        }
        bidDAO.saveBid(bid, sessionId);
        auctionSessionDAO.updateSession(session);
        return true;
    }

    public List<Bid> getBidHistory(int sessionId) {
        AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
        if (session == null || session.getBids() == null) {
            return Collections.emptyList();
        }
        return session.getBids();
    }
}
