package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
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
        SceneNavigator.loadScene(SceneNavigator.REGISTER, "register");
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

            //Đăng nhập mặc định là bidder_dashboard
            SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "BIDDER_HOME");
        } else {
            String err = protocol.lastError(null);
            ifError.setText((err == null || err.isBlank())
                    ? "Sai tài khoản / mật khẩu hoặc không kết nối được server."
                    : err);
        }
    }
}
