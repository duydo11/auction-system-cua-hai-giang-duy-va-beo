package com.auction.client.controller;

import com.auction.client.MockData.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.auction.client.MockData.DataStore;

import javax.sql.DataSource;
import java.io.IOException;

public class RegisterController {

    @FXML //Liên kết signup đến login.fxml
    private void handleSignInAction(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent registerRoot = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene registerScene = new Scene(registerRoot);
            stage.setScene(registerScene);
            stage.show();

        } catch (IOException e) {
            System.err.println("Không tìm thấy file Login.fxml! Kiểm tra lại đường dẫn.");
            e.printStackTrace();
        }
    }

    //Khai báo biến
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

    //Lấy dữ liệu người dùng, Đăng kí
    @FXML
    private void handleRegister(ActionEvent event) {

        ifError.setText("");//Xóa thông báo lỗi cũ
        ifSuccess.setText("");
        boolean Error =  false;

        String username = textUsername.getText();
        String email = textEmailAddress.getText();
        String password = textPassword.getText();

        //Nếu người dùng để trống ô, thông báo lỗi
        if(username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Error = true;
            ifError.setText("Please complete all fields!");
        }
        //Nếu thành công, lưu dữ liệu
        if (!Error) {
            ifSuccess.setText("Registration successful!");
            UserSession newUser = new  UserSession(username, email, password);
            DataStore.users.put(email, newUser);
        }
    }
}