package com.auction.server.dao;


import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseDAO {
    protected Connection connection;

    public BaseDAO() {
        // Lấy connection từ DatabaseConnection singleton
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * @throws SQLException nếu connection null hoặc closed
     */
    protected void checkConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            throw new SQLException("Database connection is not available");
        }
    }
}