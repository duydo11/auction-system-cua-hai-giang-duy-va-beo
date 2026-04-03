package com.auction.shared.model.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Seller extends User {
    private double rating; // Điểm uy tín của người bán

    public Seller(int id, String username, String password, String email, double rating) {
        super(id, username, password, email);
        this.rating = rating;
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}