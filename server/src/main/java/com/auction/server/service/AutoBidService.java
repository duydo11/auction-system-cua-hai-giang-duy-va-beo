package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.ClientBroadcastHub;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AutoBidConfig;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

/**
 * Auto-Bidding service.
 * 
 * Khi có bid mới, hệ thống tự động check tất cả auto-bid configs đã đăng ký
 * và đặt bid theo priority (thời gian đăng ký sớm được ưu tiên).
 * 
 * Quy tắc:
 * - Auto-bid chỉ kích hoạt khi bid mới > currentPrice
 * - Không vượt quá maxBid
 * - Priority theo thời gian đăng ký (FIFO)
 */
public class AutoBidService {
    private static final Logger logger = Logger.getLogger(AutoBidService.class.getName());

    // Map: sessionId -> Queue of AutoBidConfig (priority by registration time)
    private final Map<Integer, PriorityBlockingQueue<AutoBidConfig>> autoBidsBySession = new ConcurrentHashMap<>();

    private final AuctionSessionDAO auctionSessionDAO;
    private final BidDAO bidDAO;
    private final UserDAO userDAO;

    public AutoBidService() {
        this(new AuctionSessionDAO(), new BidDAO(), new UserDAO());
    }

    public AutoBidService(AuctionSessionDAO auctionSessionDAO, BidDAO bidDAO, UserDAO userDAO) {
        this.auctionSessionDAO = auctionSessionDAO;
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
    }

    /**
     * Đăng ký auto-bid cho một phiên.
     */
    public boolean registerAutoBid(AutoBidConfig config) {
        try {
            int sessionId = config.getSessionId();
            PriorityBlockingQueue<AutoBidConfig> queue = autoBidsBySession.computeIfAbsent(sessionId, k ->
                    new PriorityBlockingQueue<>(11, Comparator.comparing(AutoBidConfig::getRegisteredAt)));
            // Mỗi bidder chỉ nên có một cấu hình auto-bid trên một phiên; đăng ký mới sẽ thay thế cấu hình cũ.
            queue.removeIf(existing -> existing.getBidderId() == config.getBidderId());
            queue.add(config);
            
            logger.info("Auto-bid registered: bidderId=" + config.getBidderId() +
                    ", sessionId=" + sessionId +
                    ", maxBid=" + config.getMaxBid() +
                    ", increment=" + config.getIncrement());
            return true;
        } catch (Exception e) {
            logger.warning("Failed to register auto-bid: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hủy auto-bid.
     */
    public boolean cancelAutoBid(int sessionId, int bidderId) {
        try {
            PriorityBlockingQueue<AutoBidConfig> queue = autoBidsBySession.get(sessionId);
            if (queue == null) {
                return false;
            }
            
            boolean removed = queue.removeIf(config -> config.getBidderId() == bidderId);
            if (removed) {
                logger.info("Auto-bid cancelled: sessionId=" + sessionId + ", bidderId=" + bidderId);
            }
            return removed;
        } catch (Exception e) {
            logger.warning("Failed to cancel auto-bid: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xử lý auto-bid sau khi có bid mới.
     * Gọi từ BidService.placeBid() sau khi bid manual thành công.
     * 
     * @param sessionId ID phiên vừa có bid
     * @param newBidAmount Giá bid mới
     * @return true nếu có auto-bid được kích hoạt
     */
    public boolean processAutoBids(int sessionId, double newBidAmount) {
        PriorityBlockingQueue<AutoBidConfig> queue = autoBidsBySession.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return false;
        }

        AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
        if (session == null || !session.isActive()) {
            return false;
        }

        boolean anyAutoBidTriggered = false;
        List<AutoBidConfig> toRequeue = new ArrayList<>();

        // Process each auto-bid config in priority order
        while (!queue.isEmpty()) {
            AutoBidConfig config = queue.poll();
            if (config == null) break;

            // Skip if auto-bid owner is the current winner (no self-bidding)
            if (session.getWinner() != null && session.getWinner().getId() == config.getBidderId()) {
                toRequeue.add(config);
                continue;
            }

            // Calculate next auto-bid amount
            double currentPrice = session.getCurrentPrice();
            double nextBid = Math.min(currentPrice + config.getIncrement(), config.getMaxBid());

            // Check if we can outbid
            if (nextBid > currentPrice && nextBid <= config.getMaxBid()) {
                User autoBidder = userDAO.getUserById(config.getBidderId());
                if (!(autoBidder instanceof com.auction.shared.model.user.Bidder bidderAccount)
                        || bidderAccount.getAccountBalance() < nextBid) {
                    // Nếu tài khoản không đủ số dư thì giữ config, nhưng không đặt bid vượt tiền hiện có.
                    toRequeue.add(config);
                    continue;
                }

                // Place auto-bid
                Bid autoBid = new Bid(
                        bidDAO.allocateNextBidId(),
                        autoBidder,
                        session,
                        nextBid
                );
                autoBid.setTime(LocalDateTime.now());
                
                session.updateCurrentPrice(autoBid);
                bidDAO.saveBid(autoBid, sessionId);
                auctionSessionDAO.updateSession(session);

                // Broadcast update
                ClientBroadcastHub.broadcast(new Message(MessageType.AUCTION_UPDATED_PUSH, session));

                logger.info("Auto-bid triggered: bidderId=" + config.getBidderId() +
                        ", amount=" + nextBid);

                anyAutoBidTriggered = true;

                // Re-add to queue for potential further bids
                toRequeue.add(config);

                // Stop after one auto-bid to prevent infinite loop
                break;
            } else {
                // Cannot outbid, but keep config for future
                toRequeue.add(config);
            }
        }

        // Re-add configs to queue
        for (AutoBidConfig config : toRequeue) {
            queue.add(config);
        }

        return anyAutoBidTriggered;
    }

    /**
     * Lấy tất cả auto-bid configs cho một phiên (for debugging/testing).
     */
    public List<AutoBidConfig> getAutoBidsForSession(int sessionId) {
        PriorityBlockingQueue<AutoBidConfig> queue = autoBidsBySession.get(sessionId);
        if (queue == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(queue);
    }
}
