package com.auction.server.dao;

import com.auction.shared.model.Auction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO extends BaseDAO {

    public void createAuction(Auction auction) throws Exception {
        checkConnection();
        String sql = "INSERT INTO auctions (title, description, starting_price, current_price, " +
                "seller_id, status, start_date, end_date, category, total_bids) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, auction.getTitle());
            pstmt.setString(2, auction.getDescription());
            pstmt.setDouble(3, auction.getStartingPrice());
            pstmt.setDouble(4, auction.getCurrentPrice());
            pstmt.setInt(5, auction.getSellerId());
            pstmt.setString(6, auction.getStatus());
            pstmt.setString(7, auction.getStartDate());
            pstmt.setString(8, auction.getEndDate());
            pstmt.setString(9, auction.getCategory());
            pstmt.setInt(10, auction.getTotalBids());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    auction.setAuctionId(rs.getInt(1));
                }
            }
        }
    }

    public Auction getAuctionById(int auctionId) throws Exception {
        checkConnection();
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuction(rs);
                }
            }
        }
        return null;
    }

    public List<Auction> getActiveAuctions() throws Exception {
        checkConnection();
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = 'ACTIVE'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                auctions.add(mapResultSetToAuction(rs));
            }
        }
        return auctions;
    }

    public List<Auction> getAuctionsByCategory(String category) throws Exception {
        checkConnection();
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE category = ? AND status = 'ACTIVE'";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        }
        return auctions;
    }

    public void updateAuction(Auction auction) throws Exception {
        checkConnection();
        String sql = "UPDATE auctions SET title = ?, description = ?, current_price = ?, " +
                "status = ?, total_bids = ? WHERE auction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auction.getTitle());
            pstmt.setString(2, auction.getDescription());
            pstmt.setDouble(3, auction.getCurrentPrice());
            pstmt.setString(4, auction.getStatus());
            pstmt.setInt(5, auction.getTotalBids());
            pstmt.setInt(6, auction.getAuctionId());

            pstmt.executeUpdate();
        }
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setTitle(rs.getString("title"));
        auction.setDescription(rs.getString("description"));
        auction.setStartingPrice(rs.getDouble("starting_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setSellerId(rs.getInt("seller_id"));
        auction.setStatus(rs.getString("status"));
        auction.setStartDate(rs.getString("start_date"));
        auction.setEndDate(rs.getString("end_date"));
        auction.setCategory(rs.getString("category"));
        auction.setTotalBids(rs.getInt("total_bids"));
        return auction;
    }
}