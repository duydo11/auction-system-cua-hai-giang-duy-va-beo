package com.auction.client.controller;

import com.auction.client.SessionContext;
import com.auction.client.fx.SceneRealtime;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.ui.AuctionRowFactory;
import com.auction.shared.model.auction.AuctionSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class BidderDashboardController implements Initializable {

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    private final Consumer<AuctionSession> realtimeListener = s -> reloadDashboardAuctions();

    @FXML
    private Label lblUsername;
    @FXML
    private VBox dashboardAuctionRows;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var u = SessionContext.getCurrentUser();
        lblUsername.setText(u != null ? u.getUsername() : "Guest");
        SceneRealtime.attachAuctionUpdates(lblUsername, realtimeListener);
        reloadDashboardAuctions();
    }

    private void reloadDashboardAuctions() {
        dashboardAuctionRows.getChildren().clear();
        List<AuctionSession> auctions = protocol.getActiveAuctions();
        Runnable refresh = this::reloadDashboardAuctions;
        for (AuctionSession session : auctions) {
            dashboardAuctionRows.getChildren().add(AuctionRowFactory.bidRow(session, protocol, refresh));
        }
        if (auctions.isEmpty()) {
            dashboardAuctionRows.getChildren().add(new Label("Chưa có phiên đang mở — seller có thể đăng phiên mới."));
        }
    }

    @FXML
    public void switchSellerDB(MouseEvent mouseEvent) {
        try {
            Parent sellerView = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/SellerScene/SellerDashboard.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(sellerView);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file /fxml/SellerDashboard.fxml");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

    @FXML
    public void switchMyBids(MouseEvent mouseEvent) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/MyBids.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

    @FXML
    public void switchItems(MouseEvent mouseEvent) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/BidderScene/Items.fxml")));
            Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Lỗi: Đường dẫn file FXML bị sai (Null)");
            e.printStackTrace();
        }
    }

    @FXML
    private void switchWalletPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Wallet1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void switchSettingsPane(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/BidderScene/Setting1.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void switchSettingsPane1(MouseEvent event) throws IOException {
        switchSettingsPane(event);
    }
}
