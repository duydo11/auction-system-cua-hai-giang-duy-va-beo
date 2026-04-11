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

    //Tao bid
    public void saveBid(Bid bid, String sessionId) {
        String sql = "INSERT INTO bids (id, bidder_id, auction_session_id, amount, time) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bid.getId());
            ps.setString(2, bid.getBidder() != null ? bid.getBidder().getId() : null);
            ps.setString(3, sessionId);
            ps.setDouble(4, bid.getAmount());
            ps.setObject(5, bid.getTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lay thong tin bid
    public List<Bid> getBidsBySessionId(String sessionId, AuctionSession session) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_session_id = ? ORDER BY time DESC";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();

            UserDAO userDAO = new UserDAO(); // Khởi tạo UserDAO để lấy thông tin người trả giá

            while (rs.next()) {
                String bidId = rs.getString("id");
                double amount = rs.getDouble("amount");
                String bidderId = rs.getString("bidder_id");

                // Kéo thông tin Bidder từ DB
                User bidder = userDAO.getUserById(bidderId);

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
    public void deleteBid(String bidId) {
        String sql = "DELETE FROM bids WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}