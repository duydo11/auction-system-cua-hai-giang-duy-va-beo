package com.auction.shared.model.item;

import com.auction.shared.model.user.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(int id, String name, String description,
                       User seller, int warrantyMonths) {
        super(id, name, description, seller);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }
}