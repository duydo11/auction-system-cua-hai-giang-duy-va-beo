package com.auction.shared.model.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Bidder extends User {
    private double accountBalance; // Tiền dư trong tài khoản

    public Bidder(int id, String username, String password, String email, double accountBalance) {
        super(id, username, password, email);
        this.accountBalance = accountBalance;
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}