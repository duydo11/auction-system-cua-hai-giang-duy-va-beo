package com.auction.server.dao;

import com.auction.shared.model.Bid;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO extends BaseDAO {

    public void createBid(Bid bid) throws Exception {
        checkConnection();
        String sql = "INSERT INTO bids (auction_id, bidder_id, bid_amount, bid_time) " +
                "VALUES (?, ?, ?, NOW())";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, bid.getAuctionId());
            pstmt.setInt(2, bid.getBidderId());
            pstmt.setDouble(3, bid.getBidAmount());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    bid.setBidId(rs.getInt(1));
                }
            }
        }
    }

    public List<Bid> getBidsByAuctionId(int auctionId) throws Exception {
        checkConnection();
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapResultSetToBid(rs));
                }
            }
        }
        return bids;
    }

    public Bid getHighestBidForAuction(int auctionId) throws Exception {
        checkConnection();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBid(rs);
                }
            }
        }
        return null;
    }

    private Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setBidId(rs.getInt("bid_id"));
        bid.setAuctionId(rs.getInt("auction_id"));
        bid.setBidderId(rs.getInt("bidder_id"));
        bid.setBidAmount(rs.getDouble("bid_amount"));
        bid.setBidTime(rs.getString("bid_time"));
        return bid;
    }
}