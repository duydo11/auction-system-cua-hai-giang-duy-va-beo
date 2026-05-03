package com.auction.server.dao;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSessionDAO {

    // Khoi tao phien dau gia
    public void saveSession(AuctionSession session) {
        String sql = "INSERT INTO auction_sessions (id, item_id, seller_id, winner_id, " +
                "starting_price, current_price, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.getId());
            ps.setString(2, session.getItem() != null ? session.getItem().getId() : null);
            ps.setString(3, session.getSeller() != null ? session.getSeller().getId() : null);
            ps.setString(4, session.getWinner() != null ? session.getWinner().getId() : null);
            ps.setDouble(5, session.getStartingPrice());
            ps.setDouble(6, session.getCurrentPrice());
            ps.setObject(7, session.getStartTime());
            ps.setObject(8, session.getEndTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AuctionSession> findAllActiveSessions() {
        List<AuctionSession> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String sql = "SELECT id FROM auction_sessions WHERE start_time < ? AND end_time > ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, now);
            ps.setObject(2, now);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AuctionSession session = getSessionById(rs.getString("id"));
                if (session != null) {
                    list.add(session);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Lay thong tin cua phien dau gia
    public AuctionSession getSessionById(String id) {
        String sql = "SELECT * FROM auction_sessions WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fetchedId = rs.getString("id");
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
                LocalDateTime endTime = rs.getObject("end_time", LocalDateTime.class);

                // Lấy ID của các Object liên quan
                String sellerId = rs.getString("seller_id");
                String itemId = rs.getString("item_id");
                String winnerId = rs.getString("winner_id");

                // SỬ DỤNG CÁC DAO KHÁC ĐỂ KÉO FULL DỮ LIỆU
                UserDAO userDAO = new UserDAO();
                ItemDAO itemDAO = new ItemDAO();
                BidDAO bidDAO = new BidDAO();

                // Lấy các Object con
                User seller = userDAO.getUserById(sellerId);
                Item item = itemDAO.getItemById(itemId);
                User winner = (winnerId != null) ? userDAO.getUserById(winnerId) : null;

                // Khởi tạo AuctionSession với ĐẦY ĐỦ tham số
                AuctionSession session = new AuctionSession(fetchedId, seller, item, startingPrice, startTime, endTime);
                session.setCurrentPrice(rs.getDouble("current_price"));
                session.setWinner(winner);

                // Cuối cùng, kéo toàn bộ lịch sử trả giá (Bids) gắn vào phiên này
                List<Bid> bids = bidDAO.getBidsBySessionId(fetchedId, session);
                session.setBids(bids);

                return session; // Trả về 1 Object siêu to khổng lồ, đầy đủ mọi dữ liệu bên trong!
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Cap nhat phien dau gia
    public void updateSession(AuctionSession session) {
        String sql = "UPDATE auction_sessions SET current_price = ?, winner_id = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, session.getCurrentPrice());
            ps.setString(2, session.getWinner() != null ? session.getWinner().getId() : null);
            ps.setString(3, session.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Xoa
    public void deleteSession(String sessionId) {
        String sql = "DELETE FROM auction_sessions WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}