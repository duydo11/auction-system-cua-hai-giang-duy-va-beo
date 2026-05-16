package com.auction.shared.exception;

/**
 * Exception khi phiên đấu giá đã đóng.
 */
public class AuctionClosedException extends AuctionException {
    public AuctionClosedException(int sessionId) {
        super("Phiên đấu giá #" + sessionId + " đã đóng");
    }

    public AuctionClosedException(String message) {
        super(message);
    }
}
