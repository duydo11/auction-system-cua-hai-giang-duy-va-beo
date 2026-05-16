package com.auction.client.controller.AdminScene;
import com.auction.client.MockData.DataStore;
import com.auction.client.SessionContext;
import com.auction.client.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class AdminDashboardController {
    @FXML

    public void switchUser(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.U_MANAGEMENT, "user dashboard");
    }

    public void switchCategory(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.C_MANAGEMENT, "category dashboard");
    }

    public void switchLogout(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "login dashboard");
    }
}
