package com.auction.server.dao;

import com.auction.shared.model.user.User;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAO {

    public ItemDAO() {
        Connection conn = DatabaseConnection.getConnection();
        try (java.sql.Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE items ADD COLUMN image_path VARCHAR(1000)");
            } catch (SQLException ignore) {
                // Column already exists on most runs.
            }
        } catch (SQLException e) {
            System.err.println("Note: Item table init: " + e.getMessage());
        }
    }

    public int allocateNextItemId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM items";
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

    public void saveItem(Item item) {
        String sqlItem = "INSERT INTO items (id, name, description, seller_id, image_path) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement psItem = conn.prepareStatement(sqlItem)) {
                psItem.setInt(1, item.getId());
                psItem.setString(2, item.getName());
                psItem.setString(3, item.getDescription());
                // Seller chỉ cần lấy ID để làm khóa ngoại
                if (item.getSeller() != null) {
                    psItem.setInt(4, item.getSeller().getId());
                } else {
                    psItem.setNull(4, java.sql.Types.INTEGER);
                }
                psItem.setString(5, item.getImagePath());
                psItem.executeUpdate();

                if (item instanceof Vehicle) {
                    String sqlVehicle = "INSERT INTO vehicles (item_id, brand) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlVehicle)) {
                        ps.setInt(1, item.getId());
                        ps.setString(2, ((Vehicle) item).getBrand());
                        ps.executeUpdate();
                    }
                } else if (item instanceof Electronics) {
                    String sqlElec = "INSERT INTO electronics (item_id, warranty_months) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlElec)) {
                        ps.setInt(1, item.getId());
                        ps.setInt(2, ((Electronics) item).getWarrantyMonths());
                        ps.executeUpdate();
                    }
                } else if (item instanceof Art) {
                    String sqlArt = "INSERT INTO arts (item_id, author) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlArt)) {
                        ps.setInt(1, item.getId());
                        ps.setString(2, ((Art) item).getAuthor());
                        ps.executeUpdate();
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

    // Lay thong tin item, cung voi seller luon
    public Item getItemById(int id) {
        // Thêm i.seller_id vào câu SQL
        String sql = "SELECT i.id, i.name, i.description, i.seller_id, i.image_path, v.brand, e.warranty_months, a.author " +
                "FROM items i " +
                "LEFT JOIN vehicles v ON i.id = v.item_id " +
                "LEFT JOIN electronics e ON i.id = e.item_id " +
                "LEFT JOIN arts a ON i.id = a.item_id " +
                "WHERE i.id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int fetchedId = rs.getInt("id");
                String name = rs.getString("name");
                String desc = rs.getString("description");
                String imagePath = rs.getString("image_path");
                int sellerId = rs.getInt("seller_id");
                boolean hasSeller = !rs.wasNull();


                // Goi sang user dao de lay full seller
                UserDAO userDAO = new UserDAO();
                User seller = hasSeller ? userDAO.getUserById(sellerId) : null;

                Item item;
                if (rs.getString("brand") != null) {
                    item = new Vehicle(fetchedId, name, desc, seller, rs.getString("brand"));
                } else if (rs.getObject("warranty_months") != null) {
                    item = new Electronics(fetchedId, name, desc, seller, rs.getInt("warranty_months"));
                } else if (rs.getString("author") != null) {
                    item = new Art(fetchedId, name, desc, seller, rs.getString("author"));
                } else {
                    item = new Electronics(fetchedId, name, desc, seller, 12);
                }
                item.setImagePath(imagePath);
                return item;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Cap nhat item
    public void updateItem(Item item) {
        String sqlItem = "UPDATE items SET name = ?, description = ?, image_path = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement psItem = conn.prepareStatement(sqlItem)) {
                psItem.setString(1, item.getName());
                psItem.setString(2, item.getDescription());
                psItem.setString(3, item.getImagePath());
                psItem.setInt(4, item.getId());
                psItem.executeUpdate();

                // Cập nhật tùy theo class con
                if (item instanceof Vehicle) {
                    String sql = "UPDATE vehicles SET brand = ? WHERE item_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, ((Vehicle) item).getBrand());
                        ps.setInt(2, item.getId());
                        ps.executeUpdate();
                    }
                } else if (item instanceof Electronics) {
                    String sql = "UPDATE electronics SET warranty_months = ? WHERE item_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, ((Electronics) item).getWarrantyMonths());
                        ps.setInt(2, item.getId());
                        ps.executeUpdate();
                    }
                } else if (item instanceof Art) {
                    String sql = "UPDATE arts SET author = ? WHERE item_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, ((Art) item).getAuthor());
                        ps.setInt(2, item.getId());
                        ps.executeUpdate();
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

    // Xoa item
    public void deleteItem(int itemId) {
        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            try {
                conn.createStatement().executeUpdate("DELETE FROM vehicles WHERE item_id = " + itemId);
                conn.createStatement().executeUpdate("DELETE FROM electronics WHERE item_id = " + itemId);
                conn.createStatement().executeUpdate("DELETE FROM arts WHERE item_id = " + itemId);

                PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?");
                ps.setInt(1, itemId);
                ps.executeUpdate();
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

}