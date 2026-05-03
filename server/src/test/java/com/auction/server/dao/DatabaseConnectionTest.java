package com.auction.server.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatabaseConnectionTest {

    @Test
    public void testConnectionToCloud() {
        // Giả sử class kết nối của bạn tên là DatabaseConnection và có hàm getConnection()
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Nếu conn không null, tức là kết nối thành công!
            assertNotNull(conn, "Kết nối bị NULL, hãy kiểm tra lại URL, User, Password!");
            System.out.println("🎉 CHÚC MỪNG! Đã kết nối thành công tới Database trên Aiven!");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi kết nối rồi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}