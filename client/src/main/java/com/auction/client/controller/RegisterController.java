package com.auction.client.controller;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller cho màn Register.
 *
 * <p>Xử lý đăng ký user mới. Thao tác registration chạy async
 * để UI không bị đơ khi chờ server phản hồi.</p>
 */
public class RegisterController {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML private TextField textUsername;
    @FXML private TextField textEmailAddress;
    @FXML private PasswordField textPassword;
    @FXML private Label ifError;
    @FXML private Label ifSuccess;
    @FXML private Button btnRegister;

    @FXML
    private void handleSignInAction(ActionEvent event) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "LOGIN");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        ifError.setText("");
        ifSuccess.setText("");

        String username = textUsername.getText().trim();
        String email = textEmailAddress.getText().trim();
        String password = textPassword.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            ifError.setText("Please fill in all fields.");
            return;
        }

        // Disable button để tránh double-click
        if (btnRegister != null) {
            btnRegister.setDisable(true);
        }
        
        ifSuccess.setText("Registering...");

        // Gọi registration ở background thread
        FxAsync.run("register",
                () -> protocol.registerOrError(username, password, email),
                err -> {
                    if (err == null) {
                        // Registration thành công
                        ifSuccess.setText("Registration successful - Redirecting to login.");
                        
                        // Chuyển sang login screen
                        try {
                            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                            SceneNavigator.loadScene(SceneNavigator.LOGIN, "LOGIN");
                            stage.setTitle("Auction System - Login");
                            stage.setResizable(false);
                            stage.centerOnScreen();
                        } catch (Exception e) {
                            System.err.println("Failed to load Login.fxml after registration.");
                            e.printStackTrace();
                            ifError.setText("Registration complete, but failed to load login screen. Please click 'Sign in'.");
                        }
                    } else {
                        // Registration thất bại
                        ifError.setText(err.isEmpty() ? "Registration failed." : err);
                        ifSuccess.setText("");
                    }
                    
                    // Khôi phục button
                    if (btnRegister != null) {
                        btnRegister.setDisable(false);
                    }
                });
    }
}
