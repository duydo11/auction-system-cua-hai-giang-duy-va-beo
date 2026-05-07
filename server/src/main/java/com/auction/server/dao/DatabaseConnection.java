package com.auction.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private volatile static DatabaseConnection instance = null;
    private static Connection connection;

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

    // ĐÃ SỬA Ở ĐÂY: Đảm bảo connection không bao giờ bị null khi gọi
    public static Connection getConnection(){
        if (instance == null || connection == null) {
            getInstance();
        }
        return connection;
    }
}
