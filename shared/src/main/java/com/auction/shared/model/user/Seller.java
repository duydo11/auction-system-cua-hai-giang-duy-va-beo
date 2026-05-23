package com.auction.shared.model.user;

import com.auction.shared.model.item.Item;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private double rating;
    private double accountBalance;

    public Seller(int id, String username, String password, String email, double rating) {
        super(id, username, password, email);
        this.rating = rating;
        this.accountBalance = 0.0;
    }

    public Seller(int id, String username, String password, String email, double rating, double accountBalance) {
        super(id, username, password, email);
        this.rating = rating;
        this.accountBalance = accountBalance;
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }

    public Item createItem(Item item){
        return item;
    }
}