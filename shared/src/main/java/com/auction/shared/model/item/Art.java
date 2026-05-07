package com.auction.shared.model.item;

import com.auction.shared.model.user.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Art extends Item {
    private String author;
    public Art(int id, String name, String description, User seller, String author){
        super(id, name, description, seller);
        this.author = author;
    }

    @Override
    public String getItemType(){
        return "ARTS";
    }
}
