package com.auction.shared.exception;

/**
 * Base exception cho tất cả lỗi nghiệp vụ đấu giá.
 */
public class AuctionException extends RuntimeException {
    public AuctionException(String message) {
        super(message);
    }

    public AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}
