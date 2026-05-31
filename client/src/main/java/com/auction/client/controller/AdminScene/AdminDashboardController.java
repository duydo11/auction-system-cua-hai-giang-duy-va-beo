package com.auction.client.controller.AdminScene;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {
    @FXML private Label lblTotalUser;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblPendingRequest;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStatisticsAsync();
    }

    private void loadStatisticsAsync() {
        // Load số liệu thật từ server; không dùng số hardcode trong FXML nữa.
        if (lblTotalUser != null) lblTotalUser.setText("Loading...");
        if (lblTotalAuctions != null) lblTotalAuctions.setText("Loading...");
        if (lblTotalRevenue != null) lblTotalRevenue.setText("0");
        if (lblPendingRequest != null) lblPendingRequest.setText("0");

        FxAsync.run("admin-stats-load",
                () -> new AdminStats(protocol.getAllUsers(), protocol.getAllAuctions()),
                stats -> {
                    if (lblTotalUser != null) lblTotalUser.setText(String.valueOf(stats.users().size()));
                    if (lblTotalAuctions != null) lblTotalAuctions.setText(String.valueOf(stats.auctions().size()));
                    if (lblTotalRevenue != null) lblTotalRevenue.setText(formatMoney(stats.totalRevenue()));
                    if (lblPendingRequest != null) lblPendingRequest.setText("0");
                },
                error -> {
                    if (lblTotalUser != null) lblTotalUser.setText("ERR");
                    if (lblTotalAuctions != null) lblTotalAuctions.setText("ERR");
                    System.err.println("Error loading admin statistics: " + error);
                });
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f $", amount);
    }

    private record AdminStats(List<User> users, List<AuctionSession> auctions) {
        double totalRevenue() {
            return auctions.stream()
                    .filter(auction -> auction.getStatus() == com.auction.shared.model.auction.AuctionStatus.PAID)
                    .filter(auction -> auction.getWinner() != null)
                    .mapToDouble(AuctionSession::getCurrentPrice)
                    .sum();
        }
    }

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
