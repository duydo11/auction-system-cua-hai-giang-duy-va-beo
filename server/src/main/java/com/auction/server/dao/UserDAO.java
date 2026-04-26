package com.auction.server.dao;

import com.auction.shared.model.User;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class UserDAOTest {
    private UserDAO userDAO;

    @Before
    public void setUp() {
        userDAO = new UserDAO();
    }

    @Test
    public void testCreateAndGetUser() throws Exception {
        User user = new User("testuser", "test@email.com", "Test User");
        user.setPassword("password123");
        user.setPhone("0123456789");

        userDAO.createUser(user);

        assertNotNull(user.getUserId());

        User retrieved = userDAO.getUserByUsername("testuser");
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.getUsername());
    }

    @Test
    public void testUserNotFound() throws Exception {
        User user = userDAO.getUserByUsername("nonexistentuser");
        assertNull(user);
    }
}