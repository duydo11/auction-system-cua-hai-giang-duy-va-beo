package com.auction.shared.model;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho AuctionSession — kiểm tra logic cập nhật giá và trạng thái phiên.
 */
class AuctionSessionTest {
    private AuctionSession session;
    private Seller seller;
    private Bidder bidder1, bidder2;
    private Electronics item;

    @BeforeEach
    void setUp() {
        // Tạo seller
        seller = new Seller(1, "seller1", "pass123", "seller@email.com", 0.0);

        // Tạo item
        item = new Electronics(1, "Laptop", "High-end laptop", seller, 24);

        // Tạo bidders
        bidder1 = new Bidder(2, "bidder1", "pass123", "bidder1@email.com", 0.0);
        bidder2 = new Bidder(3, "bidder2", "pass123", "bidder2@email.com", 0.0);

        // Tạo phiên đấu giá: 100 giây từ giờ
        LocalDateTime now = LocalDateTime.now();
        session = new AuctionSession(
                1,
                seller,
                item,
                100.0,  // startingPrice
                now.minusSeconds(10),  // startTime (đã bắt đầu 10s trước)
                now.plusSeconds(100)   // endTime (còn 100s)
        );
    }

    /**
     * Test: Bid hợp lệ (cao hơn giá hiện tại) → giá cập nhật, winner thay đổi
     */
    @Test
    void testUpdateCurrentPrice_validBid() {
        // Arrange
        double initialPrice = session.getCurrentPrice();
        assertEquals(100.0, initialPrice, "Giá khởi đầu phải là 100");

        // Act
        Bid bid1 = new Bid(1, bidder1, session, 150.0);
        session.updateCurrentPrice(bid1);

        // Assert
        assertEquals(150.0, session.getCurrentPrice(), "Giá phải cập nhật thành 150");
        assertEquals(bidder1, session.getWinner(), "Winner phải là bidder1");
        assertEquals(1, session.getBids().size(), "Phải có 1 bid trong danh sách");
    }

    /**
     * Test: Bid thấp hơn giá hiện tại → giá không đổi, bid không được thêm
     */
    @Test
    void testUpdateCurrentPrice_bidTooLow() {
        // Arrange
        session.setCurrentPrice(100.0);
        int bidCountBefore = session.getBids().size();

        // Act
        Bid lowBid = new Bid(1, bidder1, session, 50.0);
        session.updateCurrentPrice(lowBid);

        // Assert
        assertEquals(100.0, session.getCurrentPrice(), "Giá phải giữ nguyên 100");
        assertEquals(bidCountBefore, session.getBids().size(), "Không thêm bid thấp");
        assertNull(session.getWinner(), "Winner vẫn null vì chưa có bid hợp lệ");
    }

    /**
     * Test: Bid sau khi phiên đóng → bid bị reject
     */
    @Test
    void testUpdateCurrentPrice_afterClose() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now.minusSeconds(10));  // Phiên đã đóng 10s trước
        double priceBeforeClose = session.getCurrentPrice();

        // Act
        Bid lateBid = new Bid(1, bidder1, session, 200.0);
        session.updateCurrentPrice(lateBid);

        // Assert
        assertEquals(priceBeforeClose, session.getCurrentPrice(), "Giá không đổi khi phiên đóng");
        assertEquals(0, session.getBids().size(), "Không thêm bid sau khi phiên đóng");
    }

    /**
     * Test: Bid trước khi phiên bắt đầu → bid bị reject
     */
    @Test
    void testUpdateCurrentPrice_beforeStart() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        session.setStartTime(now.plusSeconds(60));  // Phiên bắt đầu sau 60s
        session.setEndTime(now.plusSeconds(120));

        // Act
        Bid earlyBid = new Bid(1, bidder1, session, 150.0);
        session.updateCurrentPrice(earlyBid);

        // Assert
        assertEquals(100.0, session.getCurrentPrice(), "Giá không đổi khi bid trước start");
        assertEquals(0, session.getBids().size(), "Không thêm bid trước start");
    }

    /**
     * Test: isActive() — kiểm tra logic thời gian
     */
    @Test
    void testIsActive() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Case 1: Phiên đang chạy
        session.setStartTime(now.minusSeconds(30));
        session.setEndTime(now.plusSeconds(60));
        assertTrue(session.isActive(), "Phiên phải active khi nằm trong khoảng thời gian");

        // Case 2: Phiên chưa bắt đầu
        session.setStartTime(now.plusSeconds(60));
        session.setEndTime(now.plusSeconds(120));
        assertFalse(session.isActive(), "Phiên không active khi chưa bắt đầu");

        // Case 3: Phiên đã kết thúc
        session.setStartTime(now.minusSeconds(120));
        session.setEndTime(now.minusSeconds(30));
        assertFalse(session.isActive(), "Phiên không active khi đã kết thúc");
    }

    /**
     * Test: Nhiều bid liên tiếp → giá luôn tăng, winner cập nhật
     */
    @Test
    void testUpdateCurrentPrice_multipleBids() {
        // Arrange
        session.setCurrentPrice(100.0);

        // Act & Assert
        Bid bid1 = new Bid(1, bidder1, session, 150.0);
        session.updateCurrentPrice(bid1);
        assertEquals(150.0, session.getCurrentPrice());
        assertEquals(bidder1, session.getWinner());
        assertEquals(1, session.getBids().size());

        Bid bid2 = new Bid(2, bidder2, session, 200.0);
        session.updateCurrentPrice(bid2);
        assertEquals(200.0, session.getCurrentPrice());
        assertEquals(bidder2, session.getWinner());
        assertEquals(2, session.getBids().size());

        // Bid thấp hơn → không được thêm
        Bid bid3 = new Bid(3, bidder1, session, 180.0);
        session.updateCurrentPrice(bid3);
        assertEquals(200.0, session.getCurrentPrice(), "Giá không đổi");
        assertEquals(bidder2, session.getWinner(), "Winner không đổi");
        assertEquals(2, session.getBids().size(), "Bid thấp không được thêm");
    }
}
