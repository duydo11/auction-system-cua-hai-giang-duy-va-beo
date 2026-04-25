package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

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

    @FXML
    private void handleSignUpAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            Parent registerRoot = loader.load();


            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();


            Scene registerScene = new Scene(registerRoot);
            stage.setScene(registerScene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Không tìm thấy file Register.fxml! Kiểm tra lại đường dẫn.");
            e.printStackTrace();
        }
    }
}

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