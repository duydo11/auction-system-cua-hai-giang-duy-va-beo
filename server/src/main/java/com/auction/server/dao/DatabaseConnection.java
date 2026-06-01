package com.auction.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private volatile static DatabaseConnection instance = null;
    private static Connection connection;
    
    // Test connection cho H2 in-memory DB
    private static Connection testConnection = null;

    // (Nhớ đổi lại mật khẩu thật của bạn nhé, lúc nãy mình nhắc rồi đấy!)
    private static final String url = "jdbc:mysql://mysql-20b3bb5f-auction-database-5.c.aivencloud.com:21011/defaultdb?sslMode=REQUIRED";
    private static final String name = "avnadmin";
    private static final String password = "AVNS_YpT84-FOxKN3zAncQNp";

    private DatabaseConnection(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, name, password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi kết nối cơ sở dữ liệu!");
        }
    }

    public static DatabaseConnection getInstance(){
        // Sử dụng Double-checked locking cho Singleton an toàn hơn
        if (instance == null) {
            synchronized (DatabaseConnection.class){
                if (instance == null){
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the active JDBC connection.
     *
     * <p>Production uses one shared MySQL connection. DAO initialization code
     * closes only statements, not this shared connection; if MySQL closes the
     * real connection anyway, this method recreates it before serving the next
     * request.</p>
     */
    public static Connection getConnection(){
        // Nếu có test connection → dùng test connection (H2)
        if (testConnection != null) {
            return testConnection;
        }
        
        try {
            if (connection == null || connection.isClosed()) {
                synchronized (DatabaseConnection.class) {
                    if (connection == null || connection.isClosed()) {
                        instance = new DatabaseConnection();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra kết nối cơ sở dữ liệu!", e);
        }
        return connection;
    }
    
    /**
     * Dùng cho test — inject H2 in-memory connection
     */
    public static void setTestConnection(Connection conn) {
        testConnection = conn;
    }
    
    /**
     * Dùng cho test — xóa test connection sau khi test xong
     */
    public static void clearTestConnection() {
        testConnection = null;
    }
}

