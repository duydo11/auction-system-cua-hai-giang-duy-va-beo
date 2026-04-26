package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    DatabaseConfig.getDbHost(),
                    DatabaseConfig.getDbPort(),
                    DatabaseConfig.getDbName());

            this.connection = DriverManager.getConnection(
                    url,
                    DatabaseConfig.getDbUser(),
                    DatabaseConfig.getDbPassword()
            );
            System.out.println("✓ Database connected successfully");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("✗ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✓ Database connection closed");
            } catch (SQLException e) {
                System.err.println("✗ Error closing database: " + e.getMessage());
            }
        }
    }
}