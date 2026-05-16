package com.auction.shared.model.auction;

/**
 * Trạng thái của phiên đấu giá.
 * 
 * - OPEN: Phiên vừa tạo, chưa bắt đầu
 * - RUNNING: Phiên đang diễn ra (startTime <= now < endTime)
 * - FINISHED: Phiên hết giờ, chưa thanh toán
 * - PAID: Phiên đã thanh toán xong
 * - CANCELED: Phiên bị hủy
 */
public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
