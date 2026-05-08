package com.auction.client;

import com.auction.client.network.NetworkCleanup;
import com.auction.client.util.SceneNavigator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlaywriteIE-Regular.ttf"), 14);
        SceneNavigator.setStage(primaryStage); // Lưu stage lại
        SceneNavigator.loadScene(SceneNavigator.MY_LISTING, "Đăng nhập hệ thống");
    }

    @Override
    public void stop() throws Exception {
        try {
            NetworkCleanup.logoutClient();
        } finally {
            super.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}