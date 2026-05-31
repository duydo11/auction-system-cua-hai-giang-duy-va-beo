package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.item.Item;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private final AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    public List<AuctionSession> getActiveAuctions() {
        return auctionSessionDAO.findAllActiveSessions().stream()
                .map(this::normalizeStatus)
                .filter(AuctionSession::isActive)
                .toList();
    }

    public List<AuctionSession> getAllAuctions() {
        return auctionSessionDAO.findAllSessions().stream()
                .map(this::normalizeStatus)
                .toList();
    }

    public AuctionSession getSessionById(int sessionId) {
        return normalizeStatus(auctionSessionDAO.getSessionById(sessionId));
    }

    /**
     * Tạo phiên: nếu {@link AuctionSession#getId()} hoặc {@code item.id} ≤ 0 thì server cấp ID tiếp theo.
     */
    public boolean createAuction(AuctionSession session) {
        try {
            if (session.getItem() != null && session.getItem().getId() <= 0) {
                session.getItem().setId(itemDAO.allocateNextItemId());
                itemDAO.saveItem(session.getItem());
            }
            if (session.getId() <= 0) {
                session.setId(auctionSessionDAO.allocateNextSessionId());
            }
            auctionSessionDAO.saveSession(session);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin item.
     * Validate: chỉ sửa được khi không có phiên nào đang RUNNING với item này.
     */
    public boolean updateItem(Item item) {
        try {
            if (isItemInRunningSession(item.getId())) {
                System.err.println("Không thể sửa item #" + item.getId() + " — phiên đang chạy");
                return false;
            }
            itemDAO.updateItem(item);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa item theo id.
     * Admin cần xóa được sản phẩm khi test/demo, nên ta xóa các phiên và bid liên quan trước.
     */
    public boolean deleteItem(int itemId) {
        try {
            // Không xóa item trực tiếp trước, vì auction_sessions/bids có thể đang tham chiếu tới item này.
            auctionSessionDAO.deleteSessionsByItemId(itemId);
            itemDAO.deleteItem(itemId);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra item có đang được dùng trong phiên RUNNING không.
     */
    private boolean isItemInRunningSession(int itemId) {
        List<AuctionSession> activeSessions = auctionSessionDAO.findAllActiveSessions();
        return activeSessions.stream()
                .anyMatch(s -> s.getItem() != null && s.getItem().getId() == itemId
                        && s.getStatus() == AuctionStatus.RUNNING);
    }

    /**
     * Đóng phiên nếu hết giờ.
     * - Kiểm tra endTime < now
     * - Chuyển status từ RUNNING → FINISHED
     * - Xác định winner (người bid cao nhất)
     * - Lưu lại DB
     * 
     * @param sessionId ID phiên cần kiểm tra
     * @return true nếu phiên được đóng, false nếu phiên vẫn còn hoạt động
     */
    public boolean closeAuctionIfExpired(int sessionId) {
        try {
            AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
            if (session == null) {
                return false;
            }
            AuctionStatus before = session.getStatus();
            AuctionSession settled = settleAuctionIfExpired(session);
            return settled != null && before != settled.getStatus();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    private AuctionSession normalizeStatus(AuctionSession session) {
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            return session;
        }
        AuctionStatus current = session.getStatus();
        if (current == AuctionStatus.CANCELED || current == AuctionStatus.PAID || current == AuctionStatus.FINISHED) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(session.getEndTime())) {
            return settleAuctionIfExpired(session);
        }

        AuctionStatus normalized = now.isBefore(session.getStartTime()) ? AuctionStatus.OPEN : AuctionStatus.RUNNING;
        if (current != normalized) {
            session.setStatus(normalized);
            auctionSessionDAO.updateSession(session);
        }
        return session;
    }

    private AuctionSession settleAuctionIfExpired(AuctionSession session) {
        if (session == null || session.getEndTime() == null || LocalDateTime.now().isBefore(session.getEndTime())) {
            return session;
        }
        AuctionStatus current = session.getStatus();
        if (current == AuctionStatus.CANCELED || current == AuctionStatus.PAID || current == AuctionStatus.FINISHED) {
            return session;
        }

        if (session.getWinner() == null) {
            session.setStatus(AuctionStatus.FINISHED);
            auctionSessionDAO.updateSession(session);
            return session;
        }

        // getSellerById/getBidderById đảm bảo trả về đúng type dù user vừa là bidder vừa là seller.
        com.auction.shared.model.user.Bidder latestWinner = userDAO.getBidderById(session.getWinner().getId());
        com.auction.shared.model.user.Seller latestSeller = session.getSeller() != null
                ? userDAO.getSellerById(session.getSeller().getId())
                : null;
        double price = session.getCurrentPrice();

        // Thanh toán chỉ chạy một lần trước khi chuyển status sang PAID.
        if (latestWinner != null) {
            latestWinner.setAccountBalance(latestWinner.getAccountBalance() - price);
            userDAO.updateUser(latestWinner);
            userDAO.saveTransaction(new com.auction.shared.model.user.Transaction(
                    0, latestWinner.getId(), price, "BID_SUCCESS", session.getItem().getName(), LocalDateTime.now()
            ));
            session.setWinner(latestWinner);
        }
        if (latestSeller != null) {
            latestSeller.setAccountBalance(latestSeller.getAccountBalance() + price);
            userDAO.updateUser(latestSeller);
            userDAO.saveTransaction(new com.auction.shared.model.user.Transaction(
                    0, latestSeller.getId(), price, "BID_SUCCESS", session.getItem().getName(), LocalDateTime.now()
            ));
            session.setSeller(latestSeller);
        }

        session.setStatus(AuctionStatus.PAID);
        auctionSessionDAO.updateSession(session);
        return session;
    }

    public boolean cancelAuction(int sessionId) {
        try {
            AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
            if (session == null || session.getStatus() == AuctionStatus.FINISHED
                    || session.getStatus() == AuctionStatus.CANCELED) {
                return false;
            }
            session.setStatus(AuctionStatus.CANCELED);
            auctionSessionDAO.updateSession(session);
            com.auction.server.network.ClientBroadcastHub.broadcast(
                    new com.auction.shared.protocol.Message(
                            com.auction.shared.protocol.MessageType.AUCTION_UPDATED_PUSH,
                            session
                    )
            );
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }
}
