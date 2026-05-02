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
import java.util.List;

public class AuctionSessionDAO {

    // Khoi tao phien dau gia
    public void saveSession(AuctionSession session) {
        String sql = "INSERT INTO auction_sessions (id, item_id, seller_id, winner_id, " +
                "starting_price, current_price, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, session.getId());
            if (session.getItem() != null) {
                ps.setInt(2, session.getItem().getId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            if (session.getSeller() != null) {
                ps.setInt(3, session.getSeller().getId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (session.getWinner() != null) {
                ps.setInt(4, session.getWinner().getId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setDouble(5, session.getStartingPrice());
            ps.setDouble(6, session.getCurrentPrice());
            ps.setObject(7, session.getStartTime());
            ps.setObject(8, session.getEndTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lay thong tin cua phien dau gia
    public AuctionSession getSessionById(int id) {
        String sql = "SELECT * FROM auction_sessions WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int fetchedId = rs.getInt("id");
                double startingPrice = rs.getDouble("starting_price");
                LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
                LocalDateTime endTime = rs.getObject("end_time", LocalDateTime.class);

                // Lấy ID của các Object liên quan
                int sellerId = rs.getInt("seller_id");
                boolean hasSeller = !rs.wasNull();
                int itemId = rs.getInt("item_id");
                boolean hasItem = !rs.wasNull();
                int winnerId = rs.getInt("winner_id");
                boolean hasWinner = !rs.wasNull();

                // SỬ DỤNG CÁC DAO KHÁC ĐỂ KÉO FULL DỮ LIỆU
                UserDAO userDAO = new UserDAO();
                ItemDAO itemDAO = new ItemDAO();
                BidDAO bidDAO = new BidDAO();

                // Lấy các Object con
                User seller = hasSeller ? userDAO.getUserById(sellerId) : null;
                Item item = hasItem ? itemDAO.getItemById(itemId) : null;
                User winner = hasWinner ? userDAO.getUserById(winnerId) : null;

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
            if (session.getWinner() != null) {
                ps.setInt(2, session.getWinner().getId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, session.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Xoa
    public void deleteSession(int sessionId) {
        String sql = "DELETE FROM auction_sessions WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}