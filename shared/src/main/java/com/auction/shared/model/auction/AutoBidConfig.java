package com.auction.shared.model.auction;

import com.auction.shared.model.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Cấu hình auto-bid cho một bidder trên một phiên đấu giá.
 * Khi có bid mới, hệ thống tự động đặt bid theo config này.
 *
 * Ưu tiên: theo thời gian đăng ký (registeredAt) — đăng ký trước được ưu tiên.
 * Giới hạn: không vượt quá maxBid.
 */
@Getter
@Setter
public class AutoBidConfig extends Entity {
    private int bidderId;
    private int sessionId;
    private double maxBid;
    private double increment;
    private LocalDateTime registeredAt;

    public AutoBidConfig(int id, int bidderId, int sessionId,
                         double maxBid, double increment) {
        super(id);
        this.bidderId = bidderId;
        this.sessionId = sessionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }
}
