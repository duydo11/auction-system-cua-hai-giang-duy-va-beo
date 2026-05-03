package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

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

    @FXML
    private TextField textEmailaddress;
    @FXML
    private PasswordField textPassword;
    @FXML
    private Label ifError;
    @FXML
    private Label ifSuccess;

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        ifError.setText("");
        ifSuccess.setText("");

        String username = textEmailaddress.getText().trim();
        String password = textPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            ifError.setText("Nhập username và mật khẩu.");
            return;
        }

        User user = protocol.login(username, password);
        if (user != null) {
            SessionContext.setCurrentUser(user);
            ifSuccess.setText("Đăng nhập thành công");

            String role = user.getRoleName();
            String fxmlPath = switch (role) {
                case "SELLER" -> "/fxml/SellerScene/SellerDashboard.fxml";
                case "ADMIN" -> "/fxml/BidderScene/BidderDashboard.fxml";
                default -> "/fxml/BidderScene/BidderDashboard.fxml";
            };

            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } else {
            ifError.setText("Sai tài khoản / mật khẩu hoặc không kết nối được server.");
        }
    }
}
