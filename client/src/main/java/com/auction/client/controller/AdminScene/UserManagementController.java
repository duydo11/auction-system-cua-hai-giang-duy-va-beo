package com.auction.client.controller.AdminScene;

import com.auction.client.util.SceneNavigator;
import javafx.scene.input.MouseEvent;

public class UserManagementController {
    public void switchHome(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ADMIN_DASHBOARD, "admin dashboard");
    }

    public void switchCategory(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.C_MANAGEMENT, "category dashboard");
    }

    public void switchLogout(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "login dashboard");
    }
}
