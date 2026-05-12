package com.auction.server.service;

import com.auction.server.dao.UserDAO;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho UserService — kiểm tra logic đăng ký, đăng nhập và phân quyền user.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserDAO userDAO;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDAO);
    }

    /**
     * Test: Đăng ký user mới thành công → trả true, gọi saveUser
     */
    @Test
    void testRegisterUser_success() {
        // Arrange
        when(userDAO.existsByUsername("newuser")).thenReturn(false);
        when(userDAO.allocateNextUserId()).thenReturn(1);
        doNothing().when(userDAO).saveUser(any(User.class));

        // Act
        boolean result = userService.registerUser("newuser", "pass123", "new@email.com", "BIDDER");

        // Assert
        assertTrue(result, "Đăng ký user mới phải thành công");
        verify(userDAO, times(1)).saveUser(any(Bidder.class));
    }

    /**
     * Test: Đăng ký username đã tồn tại → trả false, không gọi saveUser
     */
    @Test
    void testRegisterUser_duplicateUsername() {
        // Arrange
        when(userDAO.existsByUsername("existinguser")).thenReturn(true);

        // Act
        boolean result = userService.registerUser("existinguser", "pass123", "email@test.com", "BIDDER");

        // Assert
        assertFalse(result, "Đăng ký username đã tồn tại phải trả false");
        verify(userDAO, never()).saveUser(any());
    }

    /**
     * Test: Đăng ký với role BIDDER → tạo Bidder instance
     */
    @Test
    void testRegisterUser_bidderRole() {
        // Arrange
        when(userDAO.existsByUsername("bidder1")).thenReturn(false);
        when(userDAO.allocateNextUserId()).thenReturn(10);

        // Act
        userService.registerUser("bidder1", "pass", "bidder@email.com", "BIDDER");

        // Assert
        verify(userDAO).saveUser(argThat(user -> 
            user instanceof Bidder && 
            user.getUsername().equals("bidder1")
        ));
    }

    /**
     * Test: Đăng ký với role SELLER → tạo Seller instance
     */
    @Test
    void testRegisterUser_sellerRole() {
        // Arrange
        when(userDAO.existsByUsername("seller1")).thenReturn(false);
        when(userDAO.allocateNextUserId()).thenReturn(11);

        // Act
        userService.registerUser("seller1", "pass", "seller@email.com", "SELLER");

        // Assert
        verify(userDAO).saveUser(argThat(user -> 
            user instanceof Seller && 
            user.getUsername().equals("seller1")
        ));
    }

    /**
     * Test: Đăng ký với role ADMIN → tạo Admin instance
     */
    @Test
    void testRegisterUser_adminRole() {
        // Arrange
        when(userDAO.existsByUsername("admin1")).thenReturn(false);
        when(userDAO.allocateNextUserId()).thenReturn(12);

        // Act
        userService.registerUser("admin1", "pass", "admin@email.com", "ADMIN");

        // Assert
        verify(userDAO).saveUser(argThat(user -> 
            user instanceof Admin && 
            user.getUsername().equals("admin1")
        ));
    }

    /**
     * Test: Đăng ký với role null → mặc định là BIDDER
     */
    @Test
    void testRegisterUser_nullRole_defaultsToBidder() {
        // Arrange
        when(userDAO.existsByUsername("defaultuser")).thenReturn(false);
        when(userDAO.allocateNextUserId()).thenReturn(13);

        // Act
        userService.registerUser("defaultuser", "pass", "default@email.com", null);

        // Assert
        verify(userDAO).saveUser(argThat(user -> 
            user instanceof Bidder
        ));
    }

    /**
     * Test: Đăng ký với role lowercase → vẫn hoạt động (case-insensitive)
     */
    @Test
    void testRegisterUser_lowercaseRole() {
        // Arrange
        when(userDAO.existsByUsername("lowercase")).thenReturn(false);
        when(userDAO.allocateNextUserId()).thenReturn(14);

        // Act
        userService.registerUser("lowercase", "pass", "lower@email.com", "seller");

        // Assert - role "seller" (lowercase) phải được uppercase thành "SELLER"
        verify(userDAO).saveUser(argThat(user -> 
            user instanceof Seller
        ));
    }

    /**
     * Test: Đăng nhập thành công → trả User object
     */
    @Test
    void testLoginUser_success() {
        // Arrange
        Bidder testBidder = new Bidder(1, "testuser", "correctpass", "test@email.com", 0.0);
        when(userDAO.login("testuser", "correctpass")).thenReturn(testBidder);

        // Act
        User result = userService.loginUser("testuser", "correctpass");

        // Assert
        assertNotNull(result, "Đăng nhập thành công phải trả User object");
        assertEquals("testuser", result.getUsername());
        assertEquals("BIDDER", result.getRoleName());
    }

    /**
     * Test: Đăng nhập sai mật khẩu → trả null
     */
    @Test
    void testLoginUser_wrongPassword() {
        // Arrange
        when(userDAO.login("testuser", "wrongpass")).thenReturn(null);

        // Act
        User result = userService.loginUser("testuser", "wrongpass");

        // Assert
        assertNull(result, "Sai mật khẩu phải trả null");
    }

    /**
     * Test: Đăng nhập user không tồn tại → trả null
     */
    @Test
    void testLoginUser_userNotFound() {
        // Arrange
        when(userDAO.login("nonexistent", "pass")).thenReturn(null);

        // Act
        User result = userService.loginUser("nonexistent", "pass");

        // Assert
        assertNull(result, "User không tồn tại phải trả null");
    }

    /**
     * Test: Đăng nhập với seller → trả Seller instance
     */
    @Test
    void testLoginUser_sellerRole() {
        // Arrange
        Seller testSeller = new Seller(2, "sellerlogin", "pass", "seller@email.com", 4.5);
        when(userDAO.login("sellerlogin", "pass")).thenReturn(testSeller);

        // Act
        User result = userService.loginUser("sellerlogin", "pass");

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Seller, "Đăng nhập seller phải trả Seller instance");
        assertEquals("SELLER", result.getRoleName());
    }

    /**
     * Test: Đăng nhập với admin → trả Admin instance
     */
    @Test
    void testLoginUser_adminRole() {
        // Arrange
        Admin testAdmin = new Admin(3, "adminlogin", "pass", "admin@email.com", "SUPER_ADMIN");
        when(userDAO.login("adminlogin", "pass")).thenReturn(testAdmin);

        // Act
        User result = userService.loginUser("adminlogin", "pass");

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Admin, "Đăng nhập admin phải trả Admin instance");
        assertEquals("ADMIN", result.getRoleName());
    }
}
