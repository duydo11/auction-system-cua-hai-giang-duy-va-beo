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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit test cho BidService — kiểm tra logic đặt giá và xử lý concurrency.
 */
@ExtendWith(MockitoExtension.class)
class BidServiceTest {
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
    private AuctionSession activeSession;

    @BeforeEach
    void setUp() {
        // Inject mock DAOs vào BidService
        bidService = new BidService(auctionSessionDAO, bidDAO, userDAO);

        // Tạo dữ liệu test
        seller = new Seller(1, "seller1", "pass", "seller@email.com", 0.0);
        bidder1 = new Bidder(2, "bidder1", "pass", "b1@email.com", 0.0);
        bidder2 = new Bidder(3, "bidder2", "pass", "b2@email.com", 0.0);
        item = new Electronics(1, "Laptop", "Mô tả laptop", seller, 24);

        // Tạo phiên đang active
        LocalDateTime now = LocalDateTime.now();
        activeSession = new AuctionSession(
                10,
                seller,
                item,
                100.0,
                now.minusSeconds(30),   // bắt đầu 30s trước
                now.plusSeconds(120)    // kết thúc sau 120s
        );
    }

    /**
     * Test: Đặt giá thành công — session hợp lệ, bidder hợp lệ, giá hợp lệ
     */
    @Test
    void testPlaceBid_success() {
        // Arrange
        when(auctionSessionDAO.getSessionById(10)).thenReturn(activeSession);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(bidDAO.allocateNextBidId()).thenReturn(1);
        doNothing().when(bidDAO).saveBid(any(Bid.class), anyInt());
        doNothing().when(auctionSessionDAO).updateSession(any(AuctionSession.class));

        // Act
        boolean result = bidService.placeBid(10, 2, 150.0);

        // Assert
        assertTrue(result, "Bid hợp lệ phải trả về true");
        assertEquals(150.0, activeSession.getCurrentPrice(), "Giá phải được cập nhật thành 150");
        assertEquals(bidder1, activeSession.getWinner(), "Winner phải là bidder1");
        verify(bidDAO, times(1)).saveBid(any(Bid.class), eq(10));
        verify(auctionSessionDAO, times(1)).updateSession(activeSession);
    }

    /**
     * Test: Session không tồn tại → trả false, không gọi DB
     */
    @Test
    void testPlaceBid_sessionNotFound() {
        // Arrange
        when(auctionSessionDAO.getSessionById(999)).thenReturn(null);

        // Act
        boolean result = bidService.placeBid(999, 2, 150.0);

        // Assert
        assertFalse(result, "Session không tồn tại phải trả false");
        verify(bidDAO, never()).saveBid(any(), anyInt());
        verify(auctionSessionDAO, never()).updateSession(any());
    }

    /**
     * Test: Bidder không tồn tại → trả false
     */
    @Test
    void testPlaceBid_bidderNotFound() {
        // Arrange
        when(auctionSessionDAO.getSessionById(10)).thenReturn(activeSession);
        when(userDAO.getUserById(999)).thenReturn(null);

        // Act
        boolean result = bidService.placeBid(10, 999, 150.0);

        // Assert
        assertFalse(result, "Bidder không tồn tại phải trả false");
        verify(bidDAO, never()).saveBid(any(), anyInt());
    }

    /**
     * Test: Bid thấp hơn giá hiện tại → trả false, giá không đổi
     */
    @Test
    void testPlaceBid_bidTooLow() {
        // Arrange
        activeSession.setCurrentPrice(100.0);
        when(auctionSessionDAO.getSessionById(10)).thenReturn(activeSession);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(bidDAO.allocateNextBidId()).thenReturn(1);

        // Act
        boolean result = bidService.placeBid(10, 2, 50.0);

        // Assert
        assertFalse(result, "Bid thấp hơn giá hiện tại phải trả false");
        assertEquals(100.0, activeSession.getCurrentPrice(), "Giá phải giữ nguyên");
        verify(bidDAO, never()).saveBid(any(), anyInt());
    }

    /**
     * Test: Phiên đã hết giờ → bid bị reject
     */
    @Test
    void testPlaceBid_sessionExpired() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        activeSession.setEndTime(now.minusSeconds(10));  // Phiên đã đóng
        when(auctionSessionDAO.getSessionById(10)).thenReturn(activeSession);
        when(userDAO.getUserById(2)).thenReturn(bidder1);
        when(bidDAO.allocateNextBidId()).thenReturn(1);

        // Act
        boolean result = bidService.placeBid(10, 2, 150.0);

        // Assert
        assertFalse(result, "Phiên hết giờ phải reject bid");
        assertEquals(100.0, activeSession.getCurrentPrice(), "Giá không đổi");
        verify(bidDAO, never()).saveBid(any(), anyInt());
    }

    /**
     * Test: 10 thread bid đồng thời trên cùng 1 phiên → không lost update, không race condition.
     * Dùng in-memory session (không cần DB thật) để test synchronized lock logic.
     * Mỗi thread đặt giá tăng dần — chỉ bid cao nhất tại mỗi thời điểm được chấp nhận.
     */
    @Test
    void testPlaceBid_concurrent_noLostUpdate() throws InterruptedException {
        // Sử dụng session riêng để test concurrency (không mock — test lock thực)
        LocalDateTime now = LocalDateTime.now();
        AuctionSession concurrentSession = new AuctionSession(
                99,
                seller,
                item,
                0.0,    // startingPrice = 0 để mọi bid đều hợp lệ
                now.minusSeconds(60),
                now.plusSeconds(300)
        );

        int threadCount = 10;
        // Mỗi thread có 1 bidder riêng
        List<Bidder> bidders = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            bidders.add(new Bidder(100 + i, "bidder" + i, "pass", "b" + i + "@email.com", 0.0));
        }

        // Stub DAO cho concurrent test
        when(auctionSessionDAO.getSessionById(99)).thenReturn(concurrentSession);
        for (int i = 0; i < threadCount; i++) {
            final Bidder b = bidders.get(i);
            when(userDAO.getUserById(100 + i)).thenReturn(b);
        }
        when(bidDAO.allocateNextBidId()).thenReturn(1);
        doNothing().when(bidDAO).saveBid(any(Bid.class), anyInt());
        doNothing().when(auctionSessionDAO).updateSession(any(AuctionSession.class));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int bidderId = 100 + i;
            // Mỗi thread bid với giá khác nhau: 10, 20, 30, ..., 100
            final double amount = (i + 1) * 10.0;
            executor.submit(() -> {
                try {
                    startLatch.await();  // Chờ tất cả thread sẵn sàng
                    boolean result = bidService.placeBid(99, bidderId, amount);
                    if (result) successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();   // Kích hoạt tất cả thread cùng lúc
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert: Ít nhất 1 bid phải thành công
        assertTrue(successCount.get() >= 1, "Ít nhất 1 bid phải được chấp nhận");

        // Assert: Giá cuối phải là giá cao nhất được chấp nhận
        double finalPrice = concurrentSession.getCurrentPrice();
        assertTrue(finalPrice > 0, "Giá cuối phải lớn hơn 0");
        assertTrue(finalPrice <= 100.0, "Giá cuối không vượt quá bid cao nhất (100)");

        // Assert: Danh sách bid phải nhất quán (giá luôn tăng)
        List<Bid> bids = concurrentSession.getBids();
        for (int i = 1; i < bids.size(); i++) {
            assertTrue(
                bids.get(i).getAmount() >= bids.get(i - 1).getAmount(),
                "Giá bid phải luôn tăng — nếu fail nghĩa là lost update!"
            );
        }
    }

    /**
     * Test: Lấy lịch sử bid của session hợp lệ
     */
    @Test
    void testGetBidHistory_validSession() {
        // Arrange
        Bid bid1 = new Bid(1, bidder1, activeSession, 150.0);
        Bid bid2 = new Bid(2, bidder2, activeSession, 200.0);
        activeSession.getBids().add(bid1);
        activeSession.getBids().add(bid2);
        when(auctionSessionDAO.getSessionById(10)).thenReturn(activeSession);

        // Act
        List<Bid> history = bidService.getBidHistory(10);

        // Assert
        assertEquals(2, history.size(), "Phải có 2 bid trong lịch sử");
    }

    /**
     * Test: Lấy lịch sử bid của session không tồn tại → trả danh sách rỗng
     */
    @Test
    void testGetBidHistory_sessionNotFound() {
        // Arrange
        when(auctionSessionDAO.getSessionById(999)).thenReturn(null);

        // Act
        List<Bid> history = bidService.getBidHistory(999);

        // Assert
        assertNotNull(history, "Danh sách không được null");
        assertTrue(history.isEmpty(), "Danh sách phải rỗng khi session không tồn tại");
    }
}
