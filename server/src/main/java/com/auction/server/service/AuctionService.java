package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.shared.model.auction.AuctionSession;

import java.util.List;

public class AuctionService {
    private final AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO();

    public List<AuctionSession> getActiveAuctions() {
        return auctionSessionDAO.findAllActiveSessions();
    }

    public boolean createAuction(AuctionSession session) {
        auctionSessionDAO.saveSession(session);
        return true;
    }
}
