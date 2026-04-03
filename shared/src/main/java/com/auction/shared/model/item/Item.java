package com.auction.shared.model.item;

import com.auction.shared.model.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private int sellerId; // ID của người bán (Tham chiếu khóa ngoại DB)

    public Item(int id, String name, String description, double startingPrice, int sellerId) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    public abstract String getItemType();
}