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
    /** Local path/URI of the product image selected by the seller. */
    private String imagePath;

    public Item(int id, String name, String description, User seller) {
        super(id);
        this.name = name;
        this.description = description;
        this.seller = seller;
    }

    public Item(int id, String name, String description, User seller, String imagePath) {
        this(id, name, description, seller);
        this.imagePath = imagePath;
    }

    public abstract String getItemType();

    public String getSellerUsername() {
        return seller.getUsername();
    }
}