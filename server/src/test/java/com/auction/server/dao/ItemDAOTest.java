package com.auction.server.dao;

import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemDAOTest {

    @Test
    public void testItemCRUD() {
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();

        int sellerId = 91001;
        int itemId = 92001;

        // 1. Chuẩn bị User (Người bán) trước vì Item bắt buộc phải có khóa ngoại seller_id
        Admin seller = new Admin(sellerId, "seller_item", "123", "sell@mail.com", "MODERATOR");
        userDAO.saveUser(seller);

        try {
            // 2. TẠO ITEM (CREATE)
            Vehicle vehicle = new Vehicle(itemId, "Xe Toyota", "Xe cũ", seller, "Toyota");
            itemDAO.saveItem(vehicle);

            // 3. ĐỌC ITEM LÊN VÀ KIỂM TRA (READ)
            Item dbItem = itemDAO.getItemById(itemId);
            assertNotNull(dbItem, "Lỗi: Không lưu được Item vào DB!");
            assertTrue(dbItem instanceof Vehicle, "Lỗi: Không nhận diện đúng class Vehicle!");
            assertEquals("Toyota", ((Vehicle) dbItem).getBrand());
            assertEquals(sellerId, dbItem.getSeller().getId(), "Lỗi: Không kéo được thông tin Seller!");

            // 4. CẬP NHẬT ITEM (UPDATE)
            dbItem.setName("Xe Honda");
            ((Vehicle) dbItem).setBrand("Honda");
            itemDAO.updateItem(dbItem);

            Item updatedItem = itemDAO.getItemById(itemId);
            assertEquals("Xe Honda", updatedItem.getName());
            assertEquals("Honda", ((Vehicle) updatedItem).getBrand());

        } finally {
            // 5. DỌN DẸP DB (Xóa con trước, xóa cha sau)
            itemDAO.deleteItem(itemId);
            userDAO.deleteUser(sellerId);
        }
    }
}