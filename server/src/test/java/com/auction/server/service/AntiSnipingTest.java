package com.auction.server.service;

import com.auction.server.dao.AuctionSessionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Test anti-sniping logic:
 * - Bid trong 30s cuối → endTime gia hạn +60s
 * - Bid ngoài 30s cuối → endTime không đổi
 * - Gia hạn liên tục khi bid liên tục trong vùng snipe
 */
@ExtendWith(MockitoExtension.class)
class AntiSnipingTest {
    @Mock
    private AuctionSessionDAO auctionSessionDAO;
    @Mock
    private BidDAO bidDAO;
    @Mock
    private UserDAO userDAO;

    private BidService bidService;
    private Seller seller;
    private Bidder bidder1, bidder2;
    private Electronics item;

    @BeforeEach
    void setUp() {
        bidService = new BidService(auctionSessionDAO, bidDAO, userDAO);
        seller = new Seller(1, "seller", "pass", "s@t.com", 0.0);
        bidder1 = new Bidder(2, "bidder1", "pass", "b1@t.com", 0.0);
        bidder2 = new Bidder(3, "bidder2", "pass", "b2@t.com", 0.0);
        item = new Electronics(1, "Laptop", "Desc", seller, 24);
    }

    /**
     * Bid trong 30s cuối → endTime gia hạn +60s
     */
    @Test
    void testBidInSnipeWindow_extendsEndTime() {
        // Phiên kết thúc trong 20s nữa (nằm trong vùng snipe 30s)
        LocalDateTime now = LocalDateTime.now();
        AuctionSession session = new AuctionSession(
                10, seller, item, 100.0,
                now.minusMinutes(5),    // bắt đầu 5 phút trước
                now.plusSeconds(20)     // kết thúc sau 20s → nằm trong vùng snipe
        );
        LocalDateTime originalEnd = session.getEndTime();

        when(auctionSessionDAO.getSessionById(10)).thenReturn(session);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(bidDAO.allocateNextBidId()).thenReturn(1);
        doNothing().when(bidDAO).saveBid(any(Bid.class), anyInt());
        doNothing().when(auctionSessionDAO).updateSession(any(AuctionSession.class));

        boolean result = bidService.placeBid(10, 2, 150.0);

        assertTrue(result, "Bid phải thành công");
        // endTime phải được gia hạn +60s
        assertTrue(session.getEndTime().isAfter(originalEnd),
                "endTime phải được gia hạn khi bid trong vùng snipe");
        // Kiểm tra gia hạn đúng 60s
        long extensionSec = java.time.temporal.ChronoUnit.SECONDS.between(originalEnd, session.getEndTime());
        assertEquals(60, extensionSec, "Gia hạn phải đúng 60s");
    }

    /**
     * Bid ngoài 30s cuối → endTime không đổi
     */
    @Test
    void testBidOutsideSnipeWindow_noExtension() {
        // Phiên kết thúc sau 120s (ngoài vùng snipe 30s)
        LocalDateTime now = LocalDateTime.now();
        AuctionSession session = new AuctionSession(
                10, seller, item, 100.0,
                now.minusMinutes(5),
                now.plusSeconds(120)    // kết thúc sau 120s → ngoài vùng snipe
        );
        LocalDateTime originalEnd = session.getEndTime();

        when(auctionSessionDAO.getSessionById(10)).thenReturn(session);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(bidDAO.allocateNextBidId()).thenReturn(1);
        doNothing().when(bidDAO).saveBid(any(Bid.class), anyInt());
        doNothing().when(auctionSessionDAO).updateSession(any(AuctionSession.class));

        bidService.placeBid(10, 2, 150.0);

        assertEquals(originalEnd, session.getEndTime(),
                "endTime KHÔNG được thay đổi khi bid ngoài vùng snipe");
    }

    /**
     * Gia hạn liên tục khi bid liên tục trong vùng snipe
     */
    @Test
    void testConsecutiveBidsInSnipeWindow_multipleExtensions() {
        // Phiên kết thúc trong 15s
        LocalDateTime now = LocalDateTime.now();
        AuctionSession session = new AuctionSession(
                10, seller, item, 100.0,
                now.minusMinutes(5),
                now.plusSeconds(15)     // 15s còn lại → snipe zone
        );

        when(auctionSessionDAO.getSessionById(10)).thenReturn(session);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(userDAO.getUserById(3)).thenReturn(bidder2);
        when(bidDAO.allocateNextBidId()).thenReturn(1).thenReturn(2);
        doNothing().when(bidDAO).saveBid(any(Bid.class), anyInt());
        doNothing().when(auctionSessionDAO).updateSession(any(AuctionSession.class));

        // Bid 1 → gia hạn lần 1 (+60s)
        bidService.placeBid(10, 2, 150.0);
        LocalDateTime afterFirstExtension = session.getEndTime();

        // Bid 2 → nếu vẫn nằm trong vùng snipe → gia hạn lần 2
        // Sau lần 1: endTime ≈ now + 15 + 60 = now + 75s → ngoài vùng snipe
        // → lần bid thứ 2 sẽ KHÔNG gia hạn (vì 75s > 30s)
        bidService.placeBid(10, 3, 200.0);

        // endTime lần 2 phải giữ nguyên (vì đã ra ngoài vùng snipe)
        assertEquals(afterFirstExtension, session.getEndTime(),
                "Bid lần 2 ngoài vùng snipe → endTime giữ nguyên");
    }

    /**
     * Bid đúng giây cuối cùng (edge case: 0s còn lại)
     */
    @Test
    void testBidAtExactEndTime_extends() {
        // Phiên kết thúc trong 1s
        LocalDateTime now = LocalDateTime.now();
        AuctionSession session = new AuctionSession(
                10, seller, item, 100.0,
                now.minusMinutes(5),
                now.plusSeconds(1)      // 1s còn lại → snipe zone
        );
        LocalDateTime originalEnd = session.getEndTime();

        when(auctionSessionDAO.getSessionById(10)).thenReturn(session);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(bidDAO.allocateNextBidId()).thenReturn(1);
        doNothing().when(bidDAO).saveBid(any(Bid.class), anyInt());
        doNothing().when(auctionSessionDAO).updateSession(any(AuctionSession.class));

        bidService.placeBid(10, 2, 150.0);

        assertTrue(session.getEndTime().isAfter(originalEnd),
                "Bid ở giây cuối phải kích hoạt anti-snipe");
    }
}
