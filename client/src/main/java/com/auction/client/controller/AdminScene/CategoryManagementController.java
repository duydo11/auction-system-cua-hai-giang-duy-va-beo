package com.auction.client.controller.AdminScene;

import com.auction.client.util.SceneNavigator;
import javafx.scene.input.MouseEvent;

public class CategoryManagementController {
    public void switchHome(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ADMIN_DASHBOARD, "admin dashboard");
    }

    public void switchUser(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.U_MANAGEMENT, "user dashboard");
    }

    public void switchLogout(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "login dashboard");
    }
}
