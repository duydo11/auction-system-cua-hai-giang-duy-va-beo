package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlaywriteIE-Regular.ttf"), 14);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BidderScene/BidderDashboard.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("Hệ thống đấu giá - Đăng nhập");
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file Login.fxml hoặc file FXML có lỗi cấu trúc.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}