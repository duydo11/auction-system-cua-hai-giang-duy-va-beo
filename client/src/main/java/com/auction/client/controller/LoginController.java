package com.auction.client.controller;

import com.auction.client.MockData.DataStore;
import com.auction.client.MockData.UserSession;
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
    //Nếu người dùng chưa có tài khoản, ấn đăng nhập
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
    //Khai báo biến nhập vào
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
        String email = textEmailaddress.getText();
        String password = textPassword.getText();

        if (DataStore.users.containsKey(email)) {
            UserSession userSession = DataStore.users.get(email);
            if (userSession.getPassword().equals(password)) {
                ifSuccess.setText("Login successful");

            DataStore.currentUser = userSession;

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/BidderDashboard.fxml"));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            }
        }
        else {
            ifError.setText("Login failed");
        }
    }
}
