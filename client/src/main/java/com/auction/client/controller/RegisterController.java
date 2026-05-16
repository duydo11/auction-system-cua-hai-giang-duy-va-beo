package com.auction.client.controller;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.network.NetworkCleanup;
import com.auction.client.util.SceneNavigator;
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
import java.util.Objects;

public class RegisterController {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    private void showLoginScreen(Stage stage) throws IOException {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "LOGIN");
        // Chuyển title sang tiếng Anh
        stage.setTitle("Auction System - Login");
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    //Đổi register
    @FXML
    private void handleSignInAction(ActionEvent event) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "LOGIN");
        }

    @FXML
    private TextField textUsername;
    @FXML
    private TextField textEmailAddress;
    @FXML
    private PasswordField textPassword;
    @FXML
    private Label ifError;
    @FXML
    private Label ifSuccess;

    @FXML
    private void handleRegister(ActionEvent event) {

        ifError.setText("");
        ifSuccess.setText("");

        String username = textUsername.getText().trim();
        String email = textEmailAddress.getText().trim();
        String password = textPassword.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            // Chuyển: Điền đủ các ô
            ifError.setText("Please fill in all fields.");
            return;
        }

        String err = protocol.registerOrError(username, password, email);

        if (err == null) {
            // Chuyển: Đăng ký thành công
            ifSuccess.setText("Registration successful - Redirecting to login.");
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                showLoginScreen(stage);
            } catch (IOException e) {
                System.err.println("Failed to load Login.fxml after registration.");
                e.printStackTrace();
                // Chuyển thông báo lỗi fallback
                ifError.setText("Registration complete, but failed to load login screen. Please click 'Sign in'.");
            }
        } else {
            // Chuyển: Đăng ký thất bại
            ifError.setText(err.isEmpty() ? "Registration failed." : err);
        }
    }
}
