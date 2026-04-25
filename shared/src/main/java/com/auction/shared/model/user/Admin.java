package com.auction.shared.model.user;

import com.auction.shared.model.item.Item;
//import com.auction.shared.model.auction.AuctionSession;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Admin extends User {
    private String accessLevel;

    public Admin(String id, String username, String password, String email, String accessLevel) {
        super(id, username, password, email);
        this.accessLevel = accessLevel;
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }

    public void banUser(User user) {
        System.out.println("Đã khóa tài khoản: " + user.getUsername());
    }

    public void approveItem(Item item) {
        System.out.println("Đã duyệt sản phẩm: " + item.getName());
    }
}