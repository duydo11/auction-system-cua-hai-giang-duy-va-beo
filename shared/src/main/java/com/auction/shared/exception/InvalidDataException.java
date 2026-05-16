package com.auction.shared.exception;

/**
 * Exception khi dữ liệu đầu vào không hợp lệ.
 */
public class InvalidDataException extends AuctionException {
    public InvalidDataException(String fieldName, String reason) {
        super("Dữ liệu không hợp lệ ở field '" + fieldName + "': " + reason);
    }

    public InvalidDataException(String message) {
        super(message);
    }
}
