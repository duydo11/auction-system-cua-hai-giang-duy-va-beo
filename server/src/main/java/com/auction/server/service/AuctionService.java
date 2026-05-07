package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.auction.AuctionSession;

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
}
