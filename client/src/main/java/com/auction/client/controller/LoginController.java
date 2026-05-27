package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.model.user.User;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private void handleLogin(ActionEvent event) {
        ifError.setText("");
        ifSuccess.setText("");

        String username = textEmailaddress.getText().trim();
        String password = textPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            ifError.setText("Enter your username and password.");
            return;
        }

        ifSuccess.setText("Signing in...");
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                return protocol.login(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            User user = loginTask.getValue();
            if (user != null) {
                SessionContext.setCurrentUser(user);
                ifSuccess.setText("Login successful.");
                switch (user.getRoleName()) {
                    case "ADMIN" -> SceneNavigator.loadScene(SceneNavigator.ADMIN_DASHBOARD, "admin dashboard");
                    case "SELLER" -> SceneNavigator.loadScene(SceneNavigator.SELLER_DASHBOARD, "seller dashboard");
                    default -> SceneNavigator.loadScene(SceneNavigator.BIDDER_DASHBOARD, "bidder dashboard");
                }
            } else {
                String err = protocol.lastError(null);
                ifSuccess.setText("");
                ifError.setText((err == null || err.isBlank())
                        ? "Invalid username/password or the server is unavailable."
                        : err);
            }
        });

        loginTask.setOnFailed(e -> {
            ifSuccess.setText("");
            ifError.setText("Login request failed. Please try again.");
        });

        Thread worker = new Thread(loginTask, "login-task");
        worker.setDaemon(true);
        worker.start();
    }
}
