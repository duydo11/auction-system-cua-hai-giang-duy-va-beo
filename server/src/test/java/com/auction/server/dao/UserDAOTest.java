package com.auction.server.dao;

import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {

    // ==========================================
    // 1. NHÓM TEST ĐĂNG NHẬP (LOGIN)
    // ==========================================

    @Test
    public void testLoginSuccess() {
        UserDAO userDAO = new UserDAO();
        Admin testAdmin = new Admin("ADMIN_TEST_01", "testadmin", "pass123", "test@mail.com", "SUPER_ADMIN");
        userDAO.saveUser(testAdmin);

        User loggedInUser = userDAO.login("testadmin", "pass123");

        assertNotNull(loggedInUser, "Lỗi: Không lấy được thông tin đăng nhập!");
        assertEquals("ADMIN", loggedInUser.getRoleName(), "Lỗi: Không nhận diện đúng phân quyền Admin!");
        assertEquals("testadmin", loggedInUser.getUsername());

        userDAO.deleteUser("ADMIN_TEST_01"); // Dọn dẹp
    }

    @Test
    public void testLoginFailure_WrongPassword() {
        UserDAO userDAO = new UserDAO();
        Admin testAdmin = new Admin("ADMIN_TEST_02", "admin_sai_pass", "matkhau_dung", "mail@mail.com", "MODERATOR");
        userDAO.saveUser(testAdmin);

        // Thử đăng nhập với mật khẩu sai
        User loggedInUser = userDAO.login("admin_sai_pass", "matkhau_sai_hoan_toan");

        // KHẲNG ĐỊNH: Kết quả phải là NULL
        assertNull(loggedInUser, "Lỗi: Nhập sai mật khẩu nhưng hệ thống vẫn cho đăng nhập!");

        userDAO.deleteUser("ADMIN_TEST_02"); // Dọn dẹp
    }

    @Test
    public void testLoginFailure_UserNotFound() {
        UserDAO userDAO = new UserDAO();

        // Thử đăng nhập bằng một tài khoản ma không có trong DB
        User loggedInUser = userDAO.login("taikhoan_khong_ton_tai", "123456");

        // KHẲNG ĐỊNH: Kết quả phải là NULL
        assertNull(loggedInUser, "Lỗi: Tài khoản không tồn tại nhưng vẫn trả về dữ liệu!");
    }

    // ==========================================
    // 2. NHÓM TEST THÊM, SỬA, XÓA DỮ LIỆU (CRUD)
    // ==========================================

    @Test
    public void testSaveAndGetBidder() {
        UserDAO userDAO = new UserDAO();
        String testId = "BIDDER_TEST_01";
        // Tạo một người mua (Bidder) với số dư 500,000
        Bidder testBidder = new Bidder(testId, "nguoimua1", "pass123", "mua@gmail.com", 500000.0);

        // Lưu vào Database
        userDAO.saveUser(testBidder);

        // Lấy từ Database lên
        User retrievedUser = userDAO.getUserById(testId);

        assertNotNull(retrievedUser, "Lỗi: Không tìm thấy user vừa lưu vào DB!");
        assertEquals("BIDDER", retrievedUser.getRoleName(), "Lỗi: Lưu Bidder nhưng lấy lên sai Role!");

        // Ép kiểu để kiểm tra thuộc tính riêng (số dư tài khoản)
        Bidder b = (Bidder) retrievedUser;
        assertEquals(500000.0, b.getAccountBalance(), "Lỗi: Số dư tài khoản lưu vào DB bị sai lệch!");

        userDAO.deleteUser(testId); // Dọn dẹp
    }

    @Test
    public void testUpdateUser() {
        UserDAO userDAO = new UserDAO();
        String testId = "ADMIN_UPDATE_TEST";
        Admin testAdmin = new Admin(testId, "admin_cu", "pass", "cu@mail", "MODERATOR");
        userDAO.saveUser(testAdmin);

        // Tiến hành thay đổi thông tin ở trên RAM
        testAdmin.setUsername("admin_moi");
        testAdmin.setAccessLevel("SUPER_ADMIN");

        // Đẩy thông tin mới xuống Database
        userDAO.updateUser(testAdmin);

        // Lấy lại từ Database để kiểm chứng
        Admin updatedAdmin = (Admin) userDAO.getUserById(testId);

        assertEquals("admin_moi", updatedAdmin.getUsername(), "Lỗi: Cập nhật Username thất bại!");
        assertEquals("SUPER_ADMIN", updatedAdmin.getAccessLevel(), "Lỗi: Cập nhật AccessLevel thất bại!");

        userDAO.deleteUser(testId); // Dọn dẹp
    }

    @Test
    public void testDeleteUser() {
        UserDAO userDAO = new UserDAO();
        String testId = "USER_DELETE_TEST";
        Admin testAdmin = new Admin(testId, "xoa_toi_di", "pass", "del@mail", "MODERATOR");

        // Bước 1: Lưu vào
        userDAO.saveUser(testAdmin);
        assertNotNull(userDAO.getUserById(testId), "Lưu thất bại, chưa có để xóa!");

        // Bước 2: Gọi hàm xóa
        userDAO.deleteUser(testId);

        // Bước 3: Tìm lại xem còn không
        User deletedUser = userDAO.getUserById(testId);
        assertNull(deletedUser, "Lỗi: Xóa user thất bại, dữ liệu vẫn còn trong DB!");
    }
}