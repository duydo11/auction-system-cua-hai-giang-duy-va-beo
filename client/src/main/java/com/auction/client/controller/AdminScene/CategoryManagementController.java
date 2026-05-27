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
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CategoryManagementController implements Initializable {
    @FXML private TableView<AuctionSession> tableView;
    @FXML private TableColumn<AuctionSession, Number> colId;
    @FXML private TableColumn<AuctionSession, String> colProduct;
    @FXML private TableColumn<AuctionSession, String> colSeller;
    @FXML private TableColumn<AuctionSession, String> colStatus;
    @FXML private TableColumn<AuctionSession, String> colStart;
    @FXML private TableColumn<AuctionSession, String> colEnd;
    @FXML private TableColumn<AuctionSession, String> colActions;
    @FXML private Label lblMessage;
    @FXML private Button btnRefresh;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
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
        if (colStart != null) {
            colStart.setCellValueFactory(cell -> new SimpleStringProperty(formatTime(cell.getValue().getStartTime())));
        }
        if (colEnd != null) {
            colEnd.setCellValueFactory(cell -> new SimpleStringProperty(formatTime(cell.getValue().getEndTime())));
        }
        if (colActions != null) {
            colActions.setCellValueFactory(cell -> new SimpleStringProperty("Xóa"));
            // Cột action có nút xóa thật để admin có thể dọn sản phẩm lỗi khi test.
            colActions.setCellFactory(column -> new TableCell<>() {
                private final Button btnDelete = new Button("Xóa");
                {
                    btnDelete.setOnAction(event -> {
                        AuctionSession session = getTableView().getItems().get(getIndex());
                        handleDeleteAuction(session);
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btnDelete);
                }
            });
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

    private void handleDeleteAuction(AuctionSession session) {
        if (session == null || session.getItem() == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + session.getItem().getName());
        confirm.setContentText("Admin sẽ xóa sản phẩm và các phiên/bid liên quan. Bạn có chắc không?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        int itemId = session.getItem().getId();
        if (lblMessage != null) {
            lblMessage.setText("Đang xóa sản phẩm...");
        }
        // Gọi server ở background để UI admin không bị đơ.
        FxAsync.run("admin-delete-item",
                () -> protocol.deleteItemOrError(itemId),
                errorMessage -> {
                    if (errorMessage == null || errorMessage.isBlank()) {
                        if (lblMessage != null) lblMessage.setText("Đã xóa sản phẩm");
                        loadAuctionsAsync();
                    } else if (lblMessage != null) {
                        lblMessage.setText("Xóa thất bại: " + errorMessage);
                    }
                },
                error -> {
                    if (lblMessage != null) lblMessage.setText("Xóa thất bại: " + error);
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

    private String formatTime(java.time.LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
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
