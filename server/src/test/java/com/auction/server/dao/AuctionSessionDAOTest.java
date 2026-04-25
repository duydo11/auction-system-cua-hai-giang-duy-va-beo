package com.auction.server.dao;

import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Electronics;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.Admin;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionSessionDAOTest {

    @Test
    public void testSessionCRUD() {
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionSessionDAO sessionDAO = new AuctionSessionDAO();

        String sellerId = "SELLER_SESSION_TEST";
        String itemId = "ITEM_ELEC_TEST";
        String sessionId = "SESSION_TEST_01";

        // 1. Chuẩn bị Dữ liệu nền (Cha)
        Admin seller = new Admin(sellerId, "seller_ss", "123", "ss@mail.com", "MODERATOR");
        userDAO.saveUser(seller);

        Electronics laptop = new Electronics(itemId, "Laptop Dell", "Mới 99%", seller, 12);
        itemDAO.saveItem(laptop);

        try {
            // 2. TẠO SESSION
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusDays(3); // Mở bán trong 3 ngày
            AuctionSession session = new AuctionSession(sessionId, seller, laptop, 1000.0, startTime, endTime);
            sessionDAO.saveSession(session);

            // 3. ĐỌC SESSION (Kiểm tra xem nó kéo các bảng khác lên đúng không)
            AuctionSession dbSession = sessionDAO.getSessionById(sessionId);
            assertNotNull(dbSession, "Lỗi: Không lưu được Session!");
            assertEquals(1000.0, dbSession.getStartingPrice());
            assertEquals(itemId, dbSession.getItem().getId(), "Lỗi: Mất thông tin Item!");
            assertEquals(sellerId, dbSession.getSeller().getId(), "Lỗi: Mất thông tin Seller!");
            assertNull(dbSession.getWinner(), "Lỗi: Session mới tạo thì chưa thể có Winner!");

            // 4. CẬP NHẬT SESSION (Có người chốt giá)
            dbSession.setCurrentPrice(1500.0);
            dbSession.setWinner(seller); // Giả lập người bán tự mua luôn cho nhanh trong test
            sessionDAO.updateSession(dbSession);

            AuctionSession updatedSession = sessionDAO.getSessionById(sessionId);
            assertEquals(1500.0, updatedSession.getCurrentPrice());
            assertNotNull(updatedSession.getWinner(), "Lỗi: Cập nhật Winner thất bại!");

        } finally {
            // 5. DỌN DẸP (Theo thứ tự từ cháu -> con -> cha)
            sessionDAO.deleteSession(sessionId);
            itemDAO.deleteItem(itemId);
            userDAO.deleteUser(sellerId);
        }
    }
}