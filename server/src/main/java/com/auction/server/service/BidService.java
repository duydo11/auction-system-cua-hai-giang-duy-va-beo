package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đặt giá qua nhiều {@link com.auction.server.network.ClientHandler} (nhiều luồng).
 * Khóa theo {@code sessionId} để hai bid cùng phiên không xen kẽ đọc/ghi trong một tick.
 */
public class BidService {
    private static final ConcurrentHashMap<Integer, Object> SESSION_BID_LOCKS = new ConcurrentHashMap<>();

    private final AuctionSessionDAO auctionSessionDAO;
    private final BidDAO bidDAO;
    private final UserDAO userDAO;

    /** Constructor mặc định — dùng trong production */
    public BidService() {
        this.auctionSessionDAO = new AuctionSessionDAO();
        this.bidDAO = new BidDAO();
        this.userDAO = new UserDAO();
    }

    /** Constructor cho test — cho phép inject mock DAO */
    public BidService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO, UserDAO userDAO) {
        this.auctionSessionDAO = auctionSessionDAO;
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
    }

    private static Object lockForSession(int sessionId) {
        return SESSION_BID_LOCKS.computeIfAbsent(sessionId, id -> new Object());
    }

    public boolean placeBid(int sessionId, int bidderId, double amount) {
        synchronized (lockForSession(sessionId)) {
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
    }

    public List<Bid> getBidHistory(int sessionId) {
        AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
        if (session == null || session.getBids() == null) {
            return Collections.emptyList();
        }
        return session.getBids();
    }
}
