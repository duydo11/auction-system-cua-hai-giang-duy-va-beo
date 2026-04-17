package com.auction.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private int userId;
    private Object params; // Tham số yêu cầu cụ thể

    public Request(int userId, Object params) {
        this.userId = userId;
        this.params = params;
        this.requestId = java.util.UUID.randomUUID().toString();
    }
}