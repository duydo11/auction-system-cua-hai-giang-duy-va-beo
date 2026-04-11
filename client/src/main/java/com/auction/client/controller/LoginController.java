package com.auction.client.controller;

import com.auction.client.network.ServerConnection;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

public class LoginController {

    // Hàm main này để test thử mạng, sau này vẽ giao diện xong sẽ xóa đi
    public static void main(String[] args) {
        System.out.println("Đang test kết nối Login...");

        // Dùng đúng class Bidder mà bạn đã tạo ở shared
        Bidder testUser = new Bidder(1, "hoang", "123", "hoang@gmail.com", 500.0);

        // Đóng gói request
        Request request = new Request("LOGIN", testUser);

        // Gửi lên server
        Response response = ServerConnection.getInstance().sendRequest(request);

        // In kết quả
        if (response != null) {
            System.out.println("Trạng thái: " + response.getStatus());
            System.out.println("Tin nhắn: " + response.getMessage());
        } else {
            System.out.println("Không nhận được phản hồi từ Server.");
        }
    }
}
//đang viết tạm hàm main test mạng ae