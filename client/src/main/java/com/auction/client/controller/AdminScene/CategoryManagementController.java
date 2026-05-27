package com.auction.client.controller.AdminScene;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.model.auction.AuctionSession;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class CategoryManagementController implements Initializable {
    @FXML private TableView<AuctionSession> tableView;
    @FXML private TableColumn<AuctionSession, Number> colId;
    @FXML private TableColumn<AuctionSession, String> colProduct;
    @FXML private TableColumn<AuctionSession, String> colSeller;
    @FXML private TableColumn<AuctionSession, String> colStatus;
    @FXML private TableColumn<AuctionSession, String> colActions;
    @FXML private Label lblMessage;
    @FXML private Button btnRefresh;

    private final ClientProtocolHandler protocol = new ClientProtocolHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadAuctionsAsync();
    }

    private void setupColumns() {
        if (colId != null) {
            colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()));
        }
        if (colProduct != null) {
            colProduct.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getItem() != null ? cell.getValue().getItem().getName() : ""
            ));
        }
        if (colSeller != null) {
            colSeller.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getSeller() != null ? cell.getValue().getSeller().getUsername() : ""
            ));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(cell -> new SimpleStringProperty(resolveStatus(cell.getValue())));
        }
        if (colActions != null) {
            colActions.setCellValueFactory(cell -> new SimpleStringProperty("Xem"));
        }
    }

    private void loadAuctionsAsync() {
        FxAsync.run("admin-category-load",
                protocol::getAllAuctions,
                auctions -> {
                    if (tableView != null) {
                        ObservableList<AuctionSession> data = FXCollections.observableArrayList(auctions);
                        tableView.setItems(data);
                    }
                    if (lblMessage != null) {
                        lblMessage.setText("Đã tải " + auctions.size() + " sản phẩm");
                    }
                },
                error -> {
                    if (lblMessage != null) {
                        lblMessage.setText("Không tải được dữ liệu category");
                    }
                });
    }

    private String resolveStatus(AuctionSession session) {
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            return "UNKNOWN";
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            return "COMING";
        }
        if (now.isAfter(session.getEndTime())) {
            return "ENDED";
        }
        return "RUNNING";
    }

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
