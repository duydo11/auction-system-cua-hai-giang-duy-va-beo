package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.ClientBroadcastHub;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Đặt giá qua nhiều {@link com.auction.server.network.ClientHandler} (nhiều luồng).
 * Khóa theo {@code sessionId} để hai bid cùng phiên không xen kẽ đọc/ghi trong một tick.
 * 
 * Anti-sniping: Nếu bid trong 30s cuối → gia hạn +60s.
 */
public class BidService {
    private static final Logger logger = Logger.getLogger(BidService.class.getName());
    private static final ConcurrentHashMap<Integer, Object> SESSION_BID_LOCKS = new ConcurrentHashMap<>();

    // Anti-sniping config
    private static final long SNIPE_WINDOW_SEC = 30;      // Vùng snipe: 30s cuối
    private static final long EXTENSION_SEC = 60;          // Gia hạn: +60s

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
            
            // Anti-sniping: kiểm tra nếu bid trong 30s cuối
            checkAndExtendForAntiSnipe(session);
            
            auctionSessionDAO.updateSession(session);
            return true;
        }
    }

    /**
     * Anti-sniping logic: nếu bid trong 30s cuối → gia hạn +60s.
     * 
     * @param session Phiên vừa có bid
     */
    private void checkAndExtendForAntiSnipe(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = session.getEndTime();
        
        // Tính thời gian còn lại
        long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(now, endTime);
        
        // Nếu bid trong 30s cuối → gia hạn
        if (secondsRemaining >= 0 && secondsRemaining <= SNIPE_WINDOW_SEC) {
            LocalDateTime newEndTime = endTime.plusSeconds(EXTENSION_SEC);
            session.setEndTime(newEndTime);
            
            logger.info("Anti-snipe triggered for auction #" + session.getId() +
                    " | Extended by " + EXTENSION_SEC + "s | New end time: " + newEndTime);
            
            // Broadcast push thông báo gia hạn
            ClientBroadcastHub.broadcast(
                    new Message(MessageType.AUCTION_EXTENDED_PUSH, session)
            );
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

