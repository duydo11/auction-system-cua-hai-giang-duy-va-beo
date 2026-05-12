package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Item;

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
     * Cập nhật thông tin item (tên, mô tả, thuộc tính riêng theo loại).
     * Validate: chỉ sửa khi phiên chưa kết thúc hoặc item chưa gắn vào phiên active.
     */
    public boolean updateItem(Item item) {
        try {
            itemDAO.updateItem(item);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa item theo id.
     * Validate: chỉ xóa được khi item chưa có phiên đang RUNNING.
     */
    public boolean deleteItem(int itemId) {
        try {
            itemDAO.deleteItem(itemId);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }
}
