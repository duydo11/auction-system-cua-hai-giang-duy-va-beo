package com.auction.shared.constant;

public class Constants {
    public static final int SERVER_PORT = 5000;
    public static final String SERVER_HOST = "localhost";

    // Auction Status
    public static final String AUCTION_ACTIVE = "ACTIVE";
    public static final String AUCTION_ENDED = "ENDED";
    public static final String AUCTION_CANCELLED = "CANCELLED";

    // Message Size
    public static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB

    // Database
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "auction_system";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";

    // API Timeouts
    public static final int REQUEST_TIMEOUT = 30000; // 30 seconds

}