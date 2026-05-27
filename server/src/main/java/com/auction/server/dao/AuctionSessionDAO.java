package com.auction.server.dao;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemFactory;
import com.auction.shared.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSessionDAO {

    public AuctionSessionDAO() {
        Connection conn = DatabaseConnection.getConnection();
        // Chỉ đóng Statement; connection là singleton dùng chung cho các request server.
        try (java.sql.Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE auction_sessions ADD COLUMN status VARCHAR(50) DEFAULT 'OPEN'");
            } catch (SQLException ignore) {}
        } catch (SQLException e) {
            System.err.println("Note: AuctionSession table init: " + e.getMessage());
        }
    }

    public int allocateNextSessionId() {
        String sql = "SELECT COALESCE(MAX(CAST(id AS UNSIGNED)), 0) + 1 AS next_id FROM auction_sessions";
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

    // Khoi tao phien dau gia
    public void saveSession(AuctionSession session) {
        String sql = "INSERT INTO auction_sessions (id, item_id, seller_id, winner_id, " +
                "starting_price, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
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
            ps.setString(9, session.getStatus() != null ? session.getStatus().name() : "OPEN");
            ps.executeUpdate();
        } catch (SQLException e) {
            // Không được nuốt lỗi ở đây, nếu insert fail thì phía trên phải biết để không báo thành công giả.
            throw new RuntimeException("Không thể lưu phiên đấu giá vào database", e);
        }
    }

    public List<AuctionSession> findAllActiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        String sql = baseSummarySql() +
                " WHERE s.start_time < ? AND s.end_time > ? AND s.status IN ('OPEN', 'RUNNING')" +
                " ORDER BY s.end_time ASC, s.id DESC";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, now);
            ps.setObject(2, now);
            return mapSessionSummaries(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load active auction summaries", e);
        }
    }

    public List<AuctionSession> findAllSessions() {
        String sql = baseSummarySql() + " ORDER BY s.start_time DESC, s.id DESC";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return mapSessionSummaries(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load auction summaries", e);
        }
    }

    // Lay thong tin cua phien dau gia
    public AuctionSession getSessionById(int id) {
        String sql = "SELECT * FROM auction_sessions WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
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
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    try {
                        session.setStatus(AuctionStatus.valueOf(statusStr));
                    } catch (IllegalArgumentException e) {
                        session.setStatus(AuctionStatus.OPEN);
                    }
                } else {
                    session.setStatus(AuctionStatus.OPEN);
                }

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
        String sql = "UPDATE auction_sessions SET current_price = ?, winner_id = ?, status = ?, end_time = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, session.getCurrentPrice());
            if (session.getWinner() != null) {
                ps.setInt(2, session.getWinner().getId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, session.getStatus() != null ? session.getStatus().name() : "OPEN");
            ps.setObject(4, session.getEndTime());
            ps.setInt(5, session.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Xoa
    public void deleteSession(int sessionId) {
        String sql = "DELETE FROM auction_sessions WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteSessionsByItemId(int itemId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            // Xóa bid trước vì bids đang trỏ tới auction_sessions.
            try (PreparedStatement psBids = conn.prepareStatement(
                    "DELETE FROM bids WHERE auction_session_id IN (SELECT id FROM auction_sessions WHERE item_id = ?)")) {
                psBids.setInt(1, itemId);
                psBids.executeUpdate();
            }
            // Sau đó mới xóa session của item để admin delete không bị kẹt khóa ngoại.
            try (PreparedStatement psSessions = conn.prepareStatement("DELETE FROM auction_sessions WHERE item_id = ?")) {
                psSessions.setInt(1, itemId);
                psSessions.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể xóa phiên đấu giá của item", e);
        }
    }

    public List<AuctionSession> findAllUnfinishedSessions() {
        String sql = baseSummarySql() + " WHERE s.status NOT IN ('FINISHED', 'PAID', 'CANCELED')";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return mapSessionSummaries(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load unfinished auction summaries", e);
        }
    }

    private String baseSummarySql() {
        return "SELECT s.id AS session_id, s.starting_price, s.current_price, s.start_time, s.end_time, s.status, " +
                "seller.id AS seller_id, seller.username AS seller_username, seller.password AS seller_password, seller.email AS seller_email, " +
                "winner.id AS winner_id, winner.username AS winner_username, winner.password AS winner_password, winner.email AS winner_email, " +
                "i.id AS item_id, i.name AS item_name, i.description AS item_description, i.image_path, " +
                "CASE WHEN e.item_id IS NOT NULL THEN 'electronics' " +
                "WHEN a.item_id IS NOT NULL THEN 'art' " +
                "WHEN v.item_id IS NOT NULL THEN 'vehicle' ELSE 'other' END AS item_type, " +
                "COALESCE(e.warranty_months, 12) AS warranty_months, a.author, v.brand " +
                "FROM auction_sessions s " +
                "LEFT JOIN users seller ON s.seller_id = seller.id " +
                "LEFT JOIN users winner ON s.winner_id = winner.id " +
                "LEFT JOIN items i ON s.item_id = i.id " +
                "LEFT JOIN electronics e ON i.id = e.item_id " +
                "LEFT JOIN arts a ON i.id = a.item_id " +
                "LEFT JOIN vehicles v ON i.id = v.item_id";
    }

    private List<AuctionSession> mapSessionSummaries(PreparedStatement ps) throws SQLException {
        List<AuctionSession> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapSessionSummary(rs));
            }
        }
        return list;
    }

    private AuctionSession mapSessionSummary(ResultSet rs) throws SQLException {
        // Dùng summary object cho các màn list/dashboard để tránh N+1 query và không kéo bids lịch sử.
        User seller = new com.auction.shared.model.user.Seller(
                rs.getInt("seller_id"), rs.getString("seller_username"),
                rs.getString("seller_password"), rs.getString("seller_email"), 0.0);

        Item item = ItemFactory.create(rs.getString("item_type"), rs.getInt("item_id"),
                rs.getString("item_name"), rs.getString("item_description"), seller,
                resolveExtraParam(rs));
        item.setImagePath(rs.getString("image_path"));

        AuctionSession session = new AuctionSession(rs.getInt("session_id"), seller, item,
                rs.getDouble("starting_price"),
                rs.getObject("start_time", LocalDateTime.class),
                rs.getObject("end_time", LocalDateTime.class));
        session.setCurrentPrice(rs.getDouble("current_price"));
        session.setStatus(parseStatus(rs.getString("status")));

        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            session.setWinner(new com.auction.shared.model.user.Bidder(
                    winnerId, rs.getString("winner_username"),
                    rs.getString("winner_password"), rs.getString("winner_email"), 0.0));
        }
        return session;
    }

    private Object resolveExtraParam(ResultSet rs) throws SQLException {
        String type = rs.getString("item_type");
        if ("art".equals(type)) {
            return rs.getString("author");
        }
        if ("vehicle".equals(type)) {
            return rs.getString("brand");
        }
        return rs.getInt("warranty_months");
    }

    private AuctionStatus parseStatus(String statusStr) {
        if (statusStr == null) {
            return AuctionStatus.OPEN;
        }
        try {
            return AuctionStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return AuctionStatus.OPEN;
        }
    }
}