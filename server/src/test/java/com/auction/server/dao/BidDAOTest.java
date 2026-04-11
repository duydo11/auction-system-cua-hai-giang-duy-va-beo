package com.auction.server.dao;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.user.Bidder;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BidDAOTest {

    @Test
    public void testBidFunctionality() {
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
        BidDAO bidDAO = new BidDAO();

        String bidderId = "BIDDER_TEST_01";
        String sellerId = "SELLER_TEST_02";
        String itemId = "ITEM_ART_TEST";
        String sessionId = "SESSION_TEST_02";
        String bidId = "BID_TEST_01";

        // 1. Chuẩn bị bộ dữ liệu siêu to
        Bidder bidder = new Bidder(bidderId, "nguoi_mua", "123", "m@mail", 5000.0);
        Bidder seller = new Bidder(sellerId, "nguoi_ban", "123", "b@mail", 0.0);
        userDAO.saveUser(bidder);
        userDAO.saveUser(seller);

        Art painting = new Art(itemId, "Tranh Mona Lisa", "Bản fake", seller, "Da Vinci");
        itemDAO.saveItem(painting);

        AuctionSession session = new AuctionSession(sessionId, seller, painting, 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        sessionDAO.saveSession(session);

        try {
            // 2. TẠO LƯỢT TRẢ GIÁ (BID)
            Bid bid = new Bid(bidId, bidder, session, 150.0);
            bid.setTime(LocalDateTime.now());
            bidDAO.saveBid(bid, sessionId);

            // 3. ĐỌC DANH SÁCH BID THEO SESSION
            List<Bid> sessionBids = bidDAO.getBidsBySessionId(sessionId, session);

            assertFalse(sessionBids.isEmpty(), "Lỗi: Không tìm thấy lượt trả giá nào!");
            assertEquals(1, sessionBids.size(), "Lỗi: Đáng lẽ chỉ có 1 lượt trả giá!");

            Bid dbBid = sessionBids.get(0);
            assertEquals(150.0, dbBid.getAmount());
            assertEquals(bidderId, dbBid.getBidder().getId(), "Lỗi: Trả sai thông tin người mua!");

        } finally {
            // 4. DỌN DẸP (Xóa từ trên đỉnh nhánh cây xuống gốc rễ)
            bidDAO.deleteBid(bidId);
            sessionDAO.deleteSession(sessionId);
            itemDAO.deleteItem(itemId);
            userDAO.deleteUser(bidderId);
            userDAO.deleteUser(sellerId);
        }
    }
}