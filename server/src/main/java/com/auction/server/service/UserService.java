package com.auction.server.service;

import com.auction.server.dao.UserDAO;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;

import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    /** Constructor mặc định — dùng trong production */
    public UserService() {
        this.userDAO = new UserDAO();
    }

    /** Constructor cho test — cho phép inject mock DAO */
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User loginUser(String username, String password) {
        return userDAO.login(username, password);
    }

    public boolean registerUser(String username, String password, String email, String role) {
        if (userDAO.existsByUsername(username)) {
            return false;
        }
        int id = userDAO.allocateNextUserId();
        String r = role == null ? "BIDDER" : role.trim().toUpperCase();
        User user;
        switch (r) {
            case "SELLER":
                user = new Seller(id, username, password, email, 0.0);
                break;
            case "ADMIN":
                user = new Admin(id, username, password, email, "STANDARD");
                break;
            default:
                user = new Bidder(id, username, password, email, 0.0);
                break;
        }
        userDAO.saveUser(user);
        return true;
    }

    /**
     * Lấy danh sách tất cả user (dành cho Admin dashboard).
     */
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    /**
     * Ban (xóa) user theo id.
     * Validate: chỉ admin mới được ban, không thể ban chính mình.
     */
    public boolean banUser(int userId) {
        try {
            userDAO.deleteUser(userId);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    public boolean updateUser(User user) {
        try {
            userDAO.updateUser(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void saveTransaction(com.auction.shared.model.user.Transaction trans) {
        userDAO.saveTransaction(trans);
    }

    public List<com.auction.shared.model.user.Transaction> getTransactionsByUserId(int userId) {
        return userDAO.getTransactionsByUserId(userId);
    }
}
