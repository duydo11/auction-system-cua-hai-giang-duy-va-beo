package com.auction.shared.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bidder extends User{
    private double accountBalance;
    public Bidder(int id, String username, String password, String email, double accountBalance){
        super(id, username, password, email);
        this.accountBalance = accountBalance;
    }

    @Override
    public String getRoleName(){ return "BIDDER";}

}