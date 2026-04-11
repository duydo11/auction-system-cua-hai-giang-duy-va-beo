package com.auction.shared.model.item;

import com.auction.shared.model.Entity;
import com.auction.shared.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Item extends Entity {
    private String name;
    private String description;
    private User seller;

    public Item(String id, String name, String description, User seller) {
        super(id);
        this.name = name;
        this.description = description;
        this.seller = seller;
    }

    public abstract String getItemType();
}