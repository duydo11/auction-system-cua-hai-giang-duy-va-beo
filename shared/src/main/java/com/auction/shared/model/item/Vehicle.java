package com.auction.shared.model.item;

import com.auction.shared.model.user.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vehicle extends Item {
    private String brand;
    public Vehicle(String id, String name, String description, User seller, String brand){
        super(id, name, description, seller);
        this.brand = brand;
    }

    @Override
    public String getItemType(){
        return "VEHICLE";
    }

}
