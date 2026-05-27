package com.auction.shared.protocol;

public enum MessageType {
    // Authentication
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    REGISTER_REQUEST,
    REGISTER_RESPONSE,
    LOGOUT_REQUEST,
    LOGOUT_RESPONSE,

    // Auction Operations
    VIEW_AUCTIONS_REQUEST,
    VIEW_AUCTIONS_RESPONSE,
    GET_ALL_AUCTIONS_REQUEST,
    GET_ALL_AUCTIONS_RESPONSE,
    CREATE_AUCTION_REQUEST,
    CREATE_AUCTION_RESPONSE,
    AUCTION_DETAILS_REQUEST,
    AUCTION_DETAILS_RESPONSE,

    // Bidding
    PLACE_BID_REQUEST,
    PLACE_BID_RESPONSE,
    GET_BIDS_REQUEST,
    GET_BIDS_RESPONSE,

    // Item Management (Seller)
    UPDATE_ITEM_REQUEST,
    UPDATE_ITEM_RESPONSE,
    DELETE_ITEM_REQUEST,
    DELETE_ITEM_RESPONSE,

    // Auto-Bidding
    REGISTER_AUTO_BID_REQUEST,
    REGISTER_AUTO_BID_RESPONSE,
    CANCEL_AUTO_BID_REQUEST,
    CANCEL_AUTO_BID_RESPONSE,

    // Admin
    BAN_USER_REQUEST,
    BAN_USER_RESPONSE,
    GET_ALL_USERS_REQUEST,
    GET_ALL_USERS_RESPONSE,

    // Server Push (Server → Client, no correlationId)
    /** Phiên đấu giá vừa cập nhật (sau bid). */
    AUCTION_UPDATED_PUSH,
    /** Phiên mới được tạo. */
    AUCTION_CREATED_PUSH,
    /** Phiên đã tự động đóng (hết giờ). */
    CLOSE_AUCTION_PUSH,
    /** Phiên gia hạn do anti-sniping. */
    AUCTION_EXTENDED_PUSH,

    // User
    GET_USER_INFO_REQUEST,
    GET_USER_INFO_RESPONSE,
    UPDATE_USER_REQUEST,
    UPDATE_USER_RESPONSE,

    // Transactions
    GET_TRANSACTIONS_REQUEST,
    GET_TRANSACTIONS_RESPONSE,
    SAVE_TRANSACTION_REQUEST,
    SAVE_TRANSACTION_RESPONSE,

    // Category
    GET_CATEGORIES_REQUEST,
    GET_CATEGORIES_RESPONSE,

    // Error & Acknowledgment
    ERROR,
    SUCCESS,
    ACK
}