package com.auction.server.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Lớp cơ sở cho các test dùng H2 in-memory database.
 * Khởi tạo DB schema trước khi chạy test và dọn dẹp sau khi xong.
 */
public abstract class DAOTestBase {
    private static Connection h2Connection;
    private static boolean initialized = false;

    @BeforeAll
    static void setupH2Database() throws Exception {
        if (initialized) return;
        
        // Load H2 driver
        Class.forName("org.h2.Driver");
        
        // Kết nối H2 in-memory với MySQL compatibility mode
        h2Connection = DriverManager.getConnection(
            "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            ""
        );
        
        // Đọc và thực thi schema
        InputStream schemaStream = DAOTestBase.class.getClassLoader()
            .getResourceAsStream("schema-h2.sql");
        
        if (schemaStream == null) {
            throw new RuntimeException("Không tìm thấy schema-h2.sql trong test resources");
        }
        
        String schema = new BufferedReader(new InputStreamReader(schemaStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Thực thi từng statement (tách bằng ;)
        Statement stmt = h2Connection.createStatement();
        for (String sql : schema.split(";")) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty()) {
                stmt.execute(trimmed);
            }
        }
        stmt.close();
        
        // Ghi đè DatabaseConnection để dùng H2
        DatabaseConnection.setTestConnection(h2Connection);
        
        initialized = true;
        System.out.println("[DAOTestBase] H2 in-memory database initialized successfully");
    }

    @AfterAll
    static void teardownH2Database() throws Exception {
        if (h2Connection != null) {
            // Xóa tất cả tables
            Statement stmt = h2Connection.createStatement();
            stmt.execute("DROP ALL OBJECTS");
            stmt.close();
            
            h2Connection.close();
            DatabaseConnection.clearTestConnection();
            initialized = false;
            System.out.println("[DAOTestBase] H2 in-memory database closed");
        }
    }
}
