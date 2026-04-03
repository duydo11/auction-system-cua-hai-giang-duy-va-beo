package com.auction.shared.model.item;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Electronics extends Item {
    private int warrantyMonths; // Số tháng bảo hành

    public Electronics(int id, String name, String description, double startingPrice,
                       int sellerId, int warrantyMonths) {
        super(id, name, description, startingPrice, sellerId);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }
}