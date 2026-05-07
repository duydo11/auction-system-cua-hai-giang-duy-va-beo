package com.auction.client.controller;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.network.NetworkCleanup;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @FXML
    private ComboBox<String> comboRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        comboRole.getItems().setAll("BIDDER", "SELLER", "ADMIN");
        comboRole.setValue("BIDDER");
    }

    private void showLoginScreen(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
        Parent loginRoot = loader.load();
        stage.setTitle("Hệ thống đấu giá - Đăng nhập");
        stage.setScene(new Scene(loginRoot));
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void handleSignInAction(ActionEvent event) {
        try {
            NetworkCleanup.logoutClient();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            showLoginScreen(stage);
        } catch (IOException e) {
            System.err.println("Không tìm thấy file Login.fxml! Kiểm tra lại đường dẫn.");
            e.printStackTrace();
        }
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
            ifError.setText("Điền đủ các ô.");
            return;
        }

        String role = comboRole.getValue() != null ? comboRole.getValue() : "BIDDER";

        String err = protocol.registerOrError(username, password, email, role);
        if (err == null) {
            ifSuccess.setText("Đăng ký thành công — chuyển sang đăng nhập.");
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                showLoginScreen(stage);
            } catch (IOException e) {
                System.err.println("Không mở được Login.fxml sau đăng ký.");
                e.printStackTrace();
                ifError.setText("Đăng ký xong nhưng không load màn đăng nhập — bấm Sign in.");
            }
        } else {
            ifError.setText(err.isEmpty() ? "Đăng ký thất bại." : err);
        }
    }
}
