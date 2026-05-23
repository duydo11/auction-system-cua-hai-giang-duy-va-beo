package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.ClientBroadcastHub;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduler tự động đóng phiên đấu giá hết hạn.
 * Chạy mỗi 10 giây, scan DB → đóng phiên hết giờ → broadcast push.
 *
 * Sử dụng:
 * <pre>
 * AuctionScheduler.getInstance().start();
 * </pre>
 */
public class AuctionScheduler {
    private static final Logger logger = Logger.getLogger(AuctionScheduler.class.getName());
    private static final int SCAN_INTERVAL_SECONDS = 10;

    private static volatile AuctionScheduler instance;

    private final ScheduledExecutorService scheduler;
    private final AuctionSessionDAO auctionSessionDAO;
    private volatile boolean running = false;

    private AuctionScheduler() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionScheduler");
            t.setDaemon(true);  // Tự tắt khi server shutdown
            return t;
        });
        this.auctionSessionDAO = new AuctionSessionDAO();
    }

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            synchronized (AuctionScheduler.class) {
                if (instance == null) {
                    instance = new AuctionScheduler();
                }
            }
        }
        return instance;
    }

    /**
     * Bắt đầu scheduler. Chạy mỗi {@link #SCAN_INTERVAL_SECONDS} giây.
     */
    public void start() {
        if (running) {
            logger.info("AuctionScheduler already running");
            return;
        }
        running = true;
        scheduler.scheduleAtFixedRate(this::scanAndCloseExpired,
                SCAN_INTERVAL_SECONDS, SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.info("AuctionScheduler started — scanning every " + SCAN_INTERVAL_SECONDS + "s");
    }

    /**
     * Dừng scheduler.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("AuctionScheduler stopped");
    }

    /**
     * Scan tất cả phiên active → đóng những phiên hết giờ → broadcast push.
     */
    private void scanAndCloseExpired() {
        try {
            List<AuctionSession> activeSessions = auctionSessionDAO.findAllUnfinishedSessions();
            LocalDateTime now = LocalDateTime.now();
            UserDAO userDAO = new UserDAO();

            for (AuctionSession session : activeSessions) {
                if (now.isAfter(session.getEndTime())) {
                    User winner = session.getWinner();
                    User seller = session.getSeller();
                    double price = session.getCurrentPrice();

                    if (winner != null) {
                        session.setStatus(AuctionStatus.PAID);

                        // Deduct from winner
                        if (winner instanceof com.auction.shared.model.user.Bidder) {
                            com.auction.shared.model.user.Bidder bidder = (com.auction.shared.model.user.Bidder) winner;
                            bidder.setAccountBalance(bidder.getAccountBalance() - price);
                            userDAO.updateUser(bidder);

                            // Save winner transaction
                            userDAO.saveTransaction(new com.auction.shared.model.user.Transaction(
                                0, bidder.getId(), price, "BID_SUCCESS", session.getItem().getName(), LocalDateTime.now()
                            ));
                        }

                        // Add to seller
                        if (seller instanceof com.auction.shared.model.user.Seller) {
                            com.auction.shared.model.user.Seller sel = (com.auction.shared.model.user.Seller) seller;
                            sel.setAccountBalance(sel.getAccountBalance() + price);
                            userDAO.updateUser(sel);

                            // Save seller transaction
                            userDAO.saveTransaction(new com.auction.shared.model.user.Transaction(
                                0, sel.getId(), price, "BID_SUCCESS", session.getItem().getName(), LocalDateTime.now()
                            ));
                        }
                    } else {
                        session.setStatus(AuctionStatus.FINISHED);
                    }

                    auctionSessionDAO.updateSession(session);

                    // Broadcast push tới client
                    ClientBroadcastHub.broadcast(
                            new Message(MessageType.CLOSE_AUCTION_PUSH, session)
                    );

                    logger.info("Auto-closed auction #" + session.getId() +
                            " | Winner: " + (winner != null ? winner.getUsername() : "None"));
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error scanning expired auctions", e);
        }
    }

    public boolean isRunning() {
        return running;
    }
}
