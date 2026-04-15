package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

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
