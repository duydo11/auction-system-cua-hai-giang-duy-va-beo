package com.auction.server.service;

import com.auction.shared.exception.*;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AuctionStatus;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.user.Seller;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho custom exception hierarchy.
 */
class ExceptionHandlingTest {

    @Test
    void testBidTooLowException_message() {
        BidTooLowException ex = new BidTooLowException(50.0, 100.0);
        assertTrue(ex.getMessage().contains("50"), "Message phải chứa bid amount");
        assertTrue(ex.getMessage().contains("100"), "Message phải chứa current price");
        assertInstanceOf(AuctionException.class, ex, "Phải là subclass của AuctionException");
    }

    @Test
    void testAuctionClosedException_message() {
        AuctionClosedException ex = new AuctionClosedException(42);
        assertTrue(ex.getMessage().contains("42"), "Message phải chứa sessionId");
        assertInstanceOf(AuctionException.class, ex);
    }

    @Test
    void testUnauthorizedRoleException_message() {
        UnauthorizedRoleException ex = new UnauthorizedRoleException("BAN_USER", "ADMIN");
        assertTrue(ex.getMessage().contains("BAN_USER"), "Message phải chứa action");
        assertTrue(ex.getMessage().contains("ADMIN"), "Message phải chứa required role");
        assertInstanceOf(AuctionException.class, ex);
    }

    @Test
    void testInvalidDataException_message() {
        InvalidDataException ex = new InvalidDataException("email", "định dạng không hợp lệ");
        assertTrue(ex.getMessage().contains("email"), "Message phải chứa field name");
        assertInstanceOf(AuctionException.class, ex);
    }

    @Test
    void testAuctionExceptionIsRuntimeException() {
        // AuctionException phải là unchecked
        AuctionException ex = new AuctionException("test");
        assertInstanceOf(RuntimeException.class, ex, "AuctionException phải extends RuntimeException");
    }

    @Test
    void testItemFactory_invalidType_throwsIllegalArgument() {
        Seller seller = new Seller(1, "seller", "pass", "seller@test.com", 0.0);
        assertThrows(IllegalArgumentException.class, () ->
                com.auction.shared.model.item.ItemFactory.create("invalid_type", 1, "Test", "Desc", seller),
                "ItemFactory phải throw IllegalArgumentException cho type không hợp lệ"
        );
    }

    @Test
    void testItemFactory_nullType_throwsIllegalArgument() {
        Seller seller = new Seller(1, "seller", "pass", "seller@test.com", 0.0);
        assertThrows(IllegalArgumentException.class, () ->
                com.auction.shared.model.item.ItemFactory.create(null, 1, "Test", "Desc", seller),
                "ItemFactory phải throw IllegalArgumentException khi type = null"
        );
    }

    @Test
    void testAuctionStatus_isActive_withFinishedStatus() {
        // Phiên bị đánh dấu FINISHED → isActive() = false dù thời gian còn
        Seller seller = new Seller(1, "s", "p", "s@t.com", 0.0);
        Electronics item = new Electronics(1, "Item", "Desc", seller, 12);
        LocalDateTime now = LocalDateTime.now();
        AuctionSession session = new AuctionSession(
                1, seller, item, 100.0,
                now.minusSeconds(30), now.plusSeconds(300)  // còn 5 phút
        );
        
        // Force set FINISHED
        session.setStatus(AuctionStatus.FINISHED);
        
        assertFalse(session.isActive(), "Phiên FINISHED phải trả isActive() = false dù còn giờ");
    }
}
