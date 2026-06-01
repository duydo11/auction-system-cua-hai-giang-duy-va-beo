package com.auction.server.dao;

import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import com.auction.shared.model.auction.AuctionSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public int allocateNextBidId() {
        String sql = "SELECT COALESCE(MAX(CAST(id AS UNSIGNED)), 0) + 1 AS next_id FROM bids";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    //Tao bid
    public void saveBid(Bid bid, int sessionId) {
        String sql = "INSERT INTO bids (id, bidder_id, auction_session_id, amount, time) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bid.getId());
            if (bid.getBidder() != null) {
                ps.setInt(2, bid.getBidder().getId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, sessionId);
            ps.setDouble(4, bid.getAmount());
            ps.setObject(5, bid.getTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lay thong tin bid
    public List<Bid> getBidsBySessionId(int sessionId, AuctionSession session) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_session_id = ? ORDER BY time ASC, id ASC";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();

            UserDAO userDAO = new UserDAO(); // Khởi tạo UserDAO để lấy thông tin người trả giá

            while (rs.next()) {
                int bidId = rs.getInt("id");
                double amount = rs.getDouble("amount");
                int bidderId = rs.getInt("bidder_id");
                boolean hasBidder = !rs.wasNull();

                // Kéo thông tin Bidder từ DB
                User bidder = hasBidder ? userDAO.getUserById(bidderId) : null;

                // Khởi tạo Bid với Full Object
                Bid bid = new Bid(bidId, bidder, session, amount);
                bid.setTime(rs.getObject("time", LocalDateTime.class)); // Ghi đè thời gian chuẩn DB

                bids.add(bid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

    // Xoa bid
    public void deleteBid(int bidId) {
        String sql = "DELETE FROM bids WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bidId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}