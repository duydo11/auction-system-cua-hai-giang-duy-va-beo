package com.auction.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private boolean success;
    private String message;
    private Object result; // Kết quả truy vấn


    public Response(String requestId, boolean success, String message) {
        this(requestId, success, message, null);
    }
}