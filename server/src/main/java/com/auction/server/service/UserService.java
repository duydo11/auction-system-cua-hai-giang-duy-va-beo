package com.auction.server.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bid;
import java.time.LocalDateTime;
import java.util.List;

public class BidService {
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;

    public BidService() {
        this.bidDAO = new BidDAO();
        this.auctionDAO = new AuctionDAO();
    }

    public boolean placeBid(int auctionId, int bidderId, double bidAmount) throws Exception {
        Auction auction = auctionDAO.getAuctionById(auctionId);

        // Kiểm tra auction có tồn tại và còn hoạt động
        if (auction == null || !auction.getStatus().equals("ACTIVE")) {
            return false;
        }

        // Kiểm tra bid amount lớn hơn current price
        if (bidAmount <= auction.getCurrentPrice()) {
            return false;
        }

        // Tạo bid mới
        Bid bid = new Bid(auctionId, bidderId, bidAmount);
        bidDAO.createBid(bid);

        // Cập nhật auction
        auction.setCurrentPrice(bidAmount);
        auction.setTotalBids(auction.getTotalBids() + 1);
        auctionDAO.updateAuction(auction);

        return true;
    }

    public Bid getHighestBid(int auctionId) throws Exception {
        return bidDAO.getHighestBidForAuction(auctionId);
    }

    public List<Bid> getBidHistory(int auctionId) throws Exception {
        return bidDAO.getBidsByAuctionId(auctionId);
    }
}