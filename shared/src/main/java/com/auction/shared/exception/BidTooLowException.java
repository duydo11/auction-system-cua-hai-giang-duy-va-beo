package com.auction.shared.exception;

/**
 * Exception khi bid thấp hơn giá hiện tại.
 */
public class BidTooLowException extends AuctionException {
    public BidTooLowException(double bidAmount, double currentPrice) {
        super(String.format("Bid %.2f quá thấp. Giá hiện tại: %.2f", bidAmount, currentPrice));
    }

    public BidTooLowException(String message) {
        super(message);
    }
}
