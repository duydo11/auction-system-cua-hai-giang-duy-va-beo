package com.auction.client.util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneNavigator {
    public static final String LOGIN = "/fxml/Login.fxml";
    public static final String REGISTER = "/fxml/Register.fxml";

    //Bidder
    public static final String BIDDER_DASHBOARD = "/fxml/BidderScene/BidderDashboard.fxml";
    public static final String WALLET1 = "/fxml/BidderScene/Wallet1.fxml";
    public static final String MY_BIDS = "/fxml/BidderScene/MyBids.fxml";
    public static final String ITEMS = "/fxml/BidderScene/Items.fxml";
    public static final String SETTING1 = "/fxml/BidderScene/Setting1.fxml";

    //Seller
    public static final String SELLER_DASHBOARD = "/fxml/SellerScene/SellerDashboard.fxml";
    public static final String MY_LISTING = "/fxml/SellerScene/MyListing.fxml";
    public static final String SHIPPING = "/fxml/SellerScene/ShippingOrder.fxml";
    public static final String SETTING2 = "/fxml/SellerScene/Setting2.fxml";
    public static final String WALLET2 = "/fxml/SellerScene/Wallet2.fxml";

    //Action
    public static final String ADD_PRODUCT = "/fxml/ActionsScene/AddProductDialog.fxml";
    public static final String WITHDRAW = "/fxml/ActionsScene/WithdrawAction.fxml";
    public static final String DEPOSIT = "/fxml/ActionsScene/DepositAction.fxml";

    //Admin
    public static final String ADMIN_DASHBOARD = "/fxml/AdminScene/AdminDashboard.fxml";
    public static final String C_MANAGEMENT = "/fxml/AdminScene/CategoryManagement.fxml";
    public static final String U_MANAGEMENT = "/fxml/AdminScene/UserManagement.fxml";

    private static Stage mainStage;
    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    //Hàm chuyển trang
    public static void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            mainStage.setTitle(title);
            mainStage.setScene(scene);
            mainStage.show();
            mainStage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi không thể tải trang: " + fxmlPath);
        }
    }
}
