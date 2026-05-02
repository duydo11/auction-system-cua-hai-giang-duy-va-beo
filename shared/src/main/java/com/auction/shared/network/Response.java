package com.auction.shared.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private String status;
    private String message;
    private Object payload;
}