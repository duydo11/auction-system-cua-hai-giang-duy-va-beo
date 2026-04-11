package com.auction.server.dao;

//import com.mysql.cj.jdbc.ConnectionImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {
    private volatile static DatabaseConnection instance = null;
    private Connection connection;
    private static final String url = "jdbc:mysql://localhost:3306/auction_db";
    private static final String name = "root";
    private static final String password = "123456";

    private DatabaseConnection(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection =    DriverManager.getConnection(url, name, password);
        }catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi kết nối cơ sở dữ liệu!");
        }
    }

    public static DatabaseConnection getInstance(){
        synchronized (DatabaseConnection.class){
            if (instance == null){
                instance = new DatabaseConnection();
                return instance;
            }
            return instance;
        }
    }

    public Connection getConnection(){
        return connection;
    }
}
