package com.auction.shared.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    private static final long serialVersionUID = 1L; // Đảm bảo tính nhất quán khi truyền Object qua mạng

    private String action;   // Loại hành động: "LOGIN", "BID", "GET_ITEMS"...
    private Object payload;  // Dữ liệu mang theo: Có thể là String, User object, hoặc Item object
    private String status;   // Trạng thái: "SUCCESS", "ERROR", hoặc để null nếu là request từ Client


    //Ví dụ cho login chẳng hạn:
    //Message cần 3 tham số (action, payload, status -> có thể để "Null" cái này khi khai báo)

}