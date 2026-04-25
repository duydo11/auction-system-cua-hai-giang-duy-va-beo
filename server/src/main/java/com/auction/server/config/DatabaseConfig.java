package com.auction.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load config.properties", ex);
        }
    }

    public static String getDbHost() {
        return properties.getProperty("db.host", "localhost");
    }

    public static int getDbPort() {
        return Integer.parseInt(properties.getProperty("db.port", "3306"));
    }

    public static String getDbName() {
        return properties.getProperty("db.name", "auction_system");
    }

    public static String getDbUser() {
        return properties.getProperty("db.user", "root");
    }

    public static String getDbPassword() {
        return properties.getProperty("db.password", "");
    }

    public static int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "5000"));
    }

    public static String getServerHost() {
        return properties.getProperty("server.host", "0.0.0.0");
    }
}