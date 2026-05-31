package com.auction.server.dao;

import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import com.auction.shared.model.user.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for users and wallet transactions.
 *
 * <p>The UI wallet screens depend on this DAO for two things:</p>
 * <ul>
 *   <li>loading the current role-specific user object with its balance,</li>
 *   <li>saving and querying transaction history for deposit, withdraw and successful auctions.</li>
 * </ul>
 */
public class UserDAO {
    private static volatile boolean initialized = false;

    public UserDAO() {
        // Chỉ chạy migration/seed một lần để admin/user/category không bị chậm vì ALTER lặp lại.
        if (initialized) {
            return;
        }
        synchronized (UserDAO.class) {
            if (initialized) {
                return;
            }
            initializeSchema();
            initialized = true;
        }
    }

    private void initializeSchema() {
        Connection conn = DatabaseConnection.getConnection();
        // Chỉ đóng Statement; connection singleton cần sống tiếp cho các request sau.
        try (java.sql.Statement stmt = conn.createStatement()) {
            // Create transactions table if not exists
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "user_id VARCHAR(255) NOT NULL, " +
                         "amount DOUBLE NOT NULL, " +
                         "type VARCHAR(50) NOT NULL, " +
                         "description VARCHAR(255), " +
                         "time DATETIME NOT NULL" +
                         ")");

            // Alter sellers table to add account_balance if not exists
            try {
                stmt.execute("ALTER TABLE sellers ADD COLUMN account_balance DOUBLE DEFAULT 0.0");
            } catch (SQLException ignore) {
                // Table might already have column, ignore
            }

            // Thêm cột ban mềm: không xóa user để tránh mất lịch sử auction/bid/transaction.
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_banned BOOLEAN DEFAULT FALSE");
            } catch (SQLException ignore) {
                // Cột đã tồn tại ở các lần chạy sau.
            }

            // Seed tài khoản mặc định cho giáo viên/tester: username admin, password admin.
            ensureDefaultAdminAccount(conn);
        } catch (SQLException e) {
            System.err.println("Note: DB initialization error: " + e.getMessage());
        }
    }

    /**
     * Ensures the default admin account exists exactly once.
     */
    private void ensureDefaultAdminAccount(Connection conn) throws SQLException {
        if (existsByUsername("admin")) {
            return;
        }

        int adminId = allocateNextUserId();
        try (PreparedStatement psUser = conn.prepareStatement(
                "INSERT INTO users (id, username, password, email) VALUES (?, ?, ?, ?)");
             PreparedStatement psAdmin = conn.prepareStatement(
                "INSERT INTO admins (user_id, access_level) VALUES (?, ?)")) {
            psUser.setInt(1, adminId);
            psUser.setString(2, "admin");
            psUser.setString(3, "admin");
            psUser.setString(4, "admin@auction.local");
            psUser.executeUpdate();

            psAdmin.setInt(1, adminId);
            psAdmin.setString(2, "SUPER_ADMIN");
            psAdmin.executeUpdate();
        }
    }

    /** ID tiếp theo cho đăng ký (schema users.id kiểu INT). */
    public int allocateNextUserId() {
        String sql = "SELECT COALESCE(MAX(CAST(id AS UNSIGNED)), 0) + 1 AS next_id FROM users";
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

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Dang nhap
    public User login(String username, String password) {
        String sql = "SELECT id, is_banned FROM users WHERE username = ? AND password = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Ban mềm: user vẫn còn trong DB để giữ lịch sử, nhưng không được đăng nhập.
                if (rs.getBoolean("is_banned")) {
                    throw new RuntimeException("bạn đã bị admin ban");
                }
                int userId = rs.getInt("id");
                return getUserById(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Sai tài khoản hoặc mật khẩu
    }

    //Tao user
    public void saveUser(User user) {
        String sqlUser = "INSERT INTO users (id, username, password, email) VALUES (?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setInt(1, user.getId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getPassword());
                ps.setString(4, user.getEmail());
                ps.executeUpdate();

                if (user instanceof Bidder) {
                    Bidder bidder = (Bidder) user;
                    String sqlBidder = "INSERT INTO bidders (user_id, account_balance) VALUES (?, ?)";
                    try (PreparedStatement psBidder = conn.prepareStatement(sqlBidder)) {
                        psBidder.setInt(1, bidder.getId());
                        psBidder.setDouble(2, bidder.getAccountBalance());
                        psBidder.executeUpdate();
                    }
                } else if (user instanceof Seller) {
                    Seller seller = (Seller) user;
                    String sqlSeller = "INSERT INTO sellers (user_id, rating, account_balance) VALUES (?, ?, ?)";
                    try (PreparedStatement psSeller = conn.prepareStatement(sqlSeller)) {
                        psSeller.setInt(1, seller.getId());
                        psSeller.setDouble(2, seller.getRating());
                        psSeller.setDouble(3, seller.getAccountBalance());
                        psSeller.executeUpdate();
                    }
                } else if (user instanceof Admin) {
                    Admin admin = (Admin) user;
                    String sqlAdmin = "INSERT INTO admins (user_id, access_level) VALUES (?, ?)";
                    try (PreparedStatement psAdmin = conn.prepareStatement(sqlAdmin)) {
                        psAdmin.setInt(1, admin.getId());
                        psAdmin.setString(2, admin.getAccessLevel());
                        psAdmin.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //lay role
    public Bidder getBidderById(int id) {
        String sql = "SELECT u.id, u.username, u.password, u.email, b.account_balance " +
                "FROM users u INNER JOIN bidders b ON u.id = b.user_id WHERE u.id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = Math.max(rs.getDouble("account_balance"), readSellerBalance(id));
                return new Bidder(
                        rs.getInt("id"), rs.getString("username"),
                        rs.getString("password"), rs.getString("email"),
                        balance
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Seller getSellerById(int id) {
        String sql = "SELECT u.id, u.username, u.password, u.email, s.rating, s.account_balance " +
                "FROM users u INNER JOIN sellers s ON u.id = s.user_id WHERE u.id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = Math.max(rs.getDouble("account_balance"), readBidderBalance(id));
                return new Seller(
                        rs.getInt("id"), rs.getString("username"),
                        rs.getString("password"), rs.getString("email"),
                        rs.getDouble("rating"), balance
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Admin getAdminById(int id) {
        String sql = "SELECT u.id, u.username, u.password, u.email, a.access_level " +
                "FROM users u INNER JOIN admins a ON u.id = a.user_id WHERE u.id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Admin(
                        rs.getInt("id"), rs.getString("username"),
                        rs.getString("password"), rs.getString("email"),
                        rs.getString("access_level")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // lay user
    public User getUserById(int id) {
        Bidder bidder = getBidderById(id);
        if (bidder != null) {
            return bidder;
        }

        Seller seller = getSellerById(id);
        if (seller != null) {
            return seller;
        }

        Admin admin = getAdminById(id);
        if (admin != null) {
            return admin;
        }

        // Fallback: nếu users có row nhưng bảng role bị thiếu, admin vẫn phải thấy user để debug/sửa dữ liệu.
        return getUserBaseAsBidder(id);
    }

    private User getUserBaseAsBidder(int id) {
        String sql = "SELECT id, username, password, email FROM users WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Bidder(rs.getInt("id"), rs.getString("username"),
                        rs.getString("password"), rs.getString("email"), 0.0);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load base user #" + id, e);
        }
        return null;
    }

    public boolean ensureSellerRole(int userId) {
        String existsSql = "SELECT 1 FROM sellers WHERE user_id = ? LIMIT 1";
        String insertSql = "INSERT INTO sellers (user_id, rating, account_balance) VALUES (?, 0.0, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement exists = conn.prepareStatement(existsSql)) {
            exists.setInt(1, userId);
            try (ResultSet rs = exists.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
            double balance = readBidderBalance(userId);
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setInt(1, userId);
                insert.setDouble(2, balance);
                return insert.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create seller role for user #" + userId, e);
        }
    }

    public boolean ensureBidderRole(int userId) {
        String existsSql = "SELECT 1 FROM bidders WHERE user_id = ? LIMIT 1";
        String insertSql = "INSERT INTO bidders (user_id, account_balance) VALUES (?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement exists = conn.prepareStatement(existsSql)) {
            exists.setInt(1, userId);
            try (ResultSet rs = exists.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
            double balance = readSellerBalance(userId);
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setInt(1, userId);
                insert.setDouble(2, balance);
                return insert.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create bidder role for user #" + userId, e);
        }
    }

    private double readBidderBalance(int userId) throws SQLException {
        return readBalance("SELECT account_balance FROM bidders WHERE user_id = ?", userId);
    }

    private double readSellerBalance(int userId) throws SQLException {
        return readBalance("SELECT account_balance FROM sellers WHERE user_id = ?", userId);
    }

    private double readBalance(String sql, int userId) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("account_balance") : 0.0;
            }
        }
    }

    // cap nhat user
    public void updateUser(User user) {
        String sqlUser = "UPDATE users SET username = ?, password = ?, email = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getEmail());
                ps.setInt(4, user.getId());
                ps.executeUpdate();

                if (user instanceof Bidder) {
                    Bidder bidder = (Bidder) user;
                    // Balance là ví chung theo user, nên cập nhật mọi role row đang tồn tại.
                    updateAllRoleBalances(conn, bidder.getId(), bidder.getAccountBalance());
                } else if (user instanceof Seller) {
                    Seller seller = (Seller) user;
                    String sqlSeller = "UPDATE sellers SET rating = ?, account_balance = ? WHERE user_id = ?";
                    try (PreparedStatement psSeller = conn.prepareStatement(sqlSeller)) {
                        psSeller.setDouble(1, seller.getRating());
                        psSeller.setDouble(2, seller.getAccountBalance());
                        psSeller.setInt(3, seller.getId());
                        psSeller.executeUpdate();
                    }
                    // Balance là ví chung theo user, nên cập nhật cả bidder row nếu user đã switch role.
                    updateAllRoleBalances(conn, seller.getId(), seller.getAccountBalance());
                } else if (user instanceof Admin) {
                    Admin admin = (Admin) user;
                    String sqlAdmin = "UPDATE admins SET access_level = ? WHERE user_id = ?";
                    try (PreparedStatement psAdmin = conn.prepareStatement(sqlAdmin)) {
                        psAdmin.setString(1, admin.getAccessLevel());
                        psAdmin.setInt(2, admin.getId());
                        psAdmin.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateAllRoleBalances(Connection conn, int userId, double balance) throws SQLException {
        try (PreparedStatement psBidder = conn.prepareStatement("UPDATE bidders SET account_balance = ? WHERE user_id = ?");
             PreparedStatement psSeller = conn.prepareStatement("UPDATE sellers SET account_balance = ? WHERE user_id = ?")) {
            psBidder.setDouble(1, balance);
            psBidder.setInt(2, userId);
            psBidder.executeUpdate();
            psSeller.setDouble(1, balance);
            psSeller.setInt(2, userId);
            psSeller.executeUpdate();
        }
    }

    public boolean banUser(int userId) {
        User user = getUserById(userId);
        if (user == null || user instanceof Admin || "admin".equalsIgnoreCase(user.getUsername())) {
            return false;
        }
        return deleteUser(userId);
    }

    public boolean deleteUser(int userId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteBidder = conn.prepareStatement("DELETE FROM bidders WHERE user_id = ?");
                 PreparedStatement deleteSeller = conn.prepareStatement("DELETE FROM sellers WHERE user_id = ?");
                 PreparedStatement deleteAdmin = conn.prepareStatement("DELETE FROM admins WHERE user_id = ?");
                 PreparedStatement deleteUser = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                // Xóa role rows trước để tránh lỗi khóa ngoại.
                deleteBidder.setInt(1, userId);
                deleteBidder.executeUpdate();
                deleteSeller.setInt(1, userId);
                deleteSeller.executeUpdate();
                deleteAdmin.setInt(1, userId);
                deleteAdmin.executeUpdate();
                deleteUser.setInt(1, userId);
                boolean deleted = deleteUser.executeUpdate() > 0;
                conn.commit();
                return deleted;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy danh sách tất cả user (dành cho Admin dashboard).
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id FROM users";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int userId = rs.getInt("id");
                User user = getUserById(userId);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public void saveTransaction(Transaction trans) {
        String sql = "INSERT INTO transactions (user_id, amount, type, description, time) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(trans.getUserId()));
            ps.setDouble(2, trans.getAmount());
            ps.setString(3, trans.getType());
            ps.setString(4, trans.getDescription());
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(trans.getTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Transaction> getTransactionsByUserId(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, user_id, amount, type, description, time FROM transactions WHERE user_id = ? ORDER BY time DESC";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(userId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getDouble("amount"),
                            rs.getString("type"),
                            rs.getString("description"),
                            rs.getTimestamp("time").toLocalDateTime()
                    );
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}