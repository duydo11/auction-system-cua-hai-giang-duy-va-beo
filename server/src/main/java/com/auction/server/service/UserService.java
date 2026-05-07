package com.auction.server.service;

import com.auction.server.dao.UserDAO;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

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
}
