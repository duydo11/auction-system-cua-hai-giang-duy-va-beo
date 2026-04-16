package com.auction.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private Object data;
    private String errorMessage;
    private boolean success;
    private String timestamp;

    // Constructor for success
    public Message(MessageType type, Object data) {
        this.type = type;
        this.data = data;
        this.success = true;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    // Constructor for error
    public Message(MessageType type, String errorMessage) {
        this.type = type;
        this.errorMessage = errorMessage;
        this.success = false;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}