package com.auction.shared.model.user;

import com.auction.shared.model.item.Item;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Seller extends User {
    private double rating;

    public Seller(String id, String username, String password, String email, double rating) {
        super(id, username, password, email);
        this.rating = rating;
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }

    public Item createItem(Item item){
        return item;
    }
}