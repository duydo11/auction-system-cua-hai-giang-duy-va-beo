package com.auction.shared.model.user;

import com.auction.shared.model.Entity;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class Transaction extends Entity {
    private static final long serialVersionUID = 1L;

    private int userId;
    private double amount;
    private String type; // DEPOSIT, WITHDRAW, BID_PAYMENT, AUCTION_SALE
    private String description;
    private LocalDateTime time;

    public Transaction() {
        super(0);
    }

    public Transaction(int id, int userId, double amount, String type, String description, LocalDateTime time) {
        super(id);
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.time = time;
    }
}
