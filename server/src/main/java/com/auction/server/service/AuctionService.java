package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.item.Item;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private final AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    public List<AuctionSession> getActiveAuctions() {
        return auctionSessionDAO.findAllActiveSessions();
    }

    public AuctionSession getSessionById(int sessionId) {
        return auctionSessionDAO.getSessionById(sessionId);
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
     * Validate: chỉ xóa được khi không có phiên nào đang RUNNING với item này.
     */
    public boolean deleteItem(int itemId) {
        try {
            if (isItemInRunningSession(itemId)) {
                System.err.println("Không thỉ xóa item #" + itemId + " — phiên đang chạy");
                return false;
            }
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

            LocalDateTime now = LocalDateTime.now();
            
            // Kiểm tra nếu hết giờ và chưa FINISHED
            if (now.isAfter(session.getEndTime()) && 
                (session.getStatus() == AuctionStatus.OPEN || session.getStatus() == AuctionStatus.RUNNING)) {
                
                // Chuyển status → FINISHED
                session.setStatus(AuctionStatus.FINISHED);
                
                // Winner đã được set trong updateCurrentPrice, không cần set lại
                // Nếu không có bid, winner = null (hợp lệ)
                
                // Lưu lại DB
                auctionSessionDAO.updateSession(session);
                
                System.out.println("Auction #" + sessionId + " closed. Winner: " + 
                    (session.getWinner() != null ? session.getWinner().getUsername() : "None"));
                
                return true;
            }
            
            return false;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }
}

