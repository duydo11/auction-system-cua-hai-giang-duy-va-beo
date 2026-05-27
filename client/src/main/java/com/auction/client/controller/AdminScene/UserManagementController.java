package com.auction.client.controller.AdminScene;

import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.client.util.SceneNavigator;
import com.auction.shared.model.user.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller cho màn User Management (admin).
 *
 * <p>Hiển thị danh sách user trong TableView, cho phép ban/unban user.
 * Tất cả thao tác network chạy async để UI không bị đơ.</p>
 *
 * <p>FXML cần có các component:</p>
 * <ul>
 *   <li>tableUsers (TableView&lt;User&gt;)</li>
 *   <li>colId (TableColumn&lt;User, Integer&gt;)</li>
 *   <li>colUsername (TableColumn&lt;User, String&gt;)</li>
 *   <li>colEmail (TableColumn&lt;User, String&gt;)</li>
 *   <li>colRole (TableColumn&lt;User, String&gt;)</li>
 *   <li>btnBan (Button)</li>
 *   <li>lblMessage (Label)</li>
 * </ul>
 */
public class UserManagementController implements Initializable {
    
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colActions;
    @FXML private Button btnBan;
    @FXML private Label lblMessage;
    
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        // Load users async để không block UI khi mở màn
        loadAllUsersAsync();
    }
    
    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        if (colId != null) {
            colId.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject()
            );
        }
        
        if (colUsername != null) {
            colUsername.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getUsername())
            );
        }
        
        if (colEmail != null) {
            colEmail.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getEmail())
            );
        }
        
        if (colStatus != null) {
            // Cột STATUS trong FXML đang được dùng để hiển thị role ngắn gọn của user.
            colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoleName())
            );
        }

        if (colActions != null) {
            // Tạm thời hiển thị action chữ để người dùng biết cột này có chức năng ban.
            colActions.setCellValueFactory(cellData -> new SimpleStringProperty("Ban"));
        }
    }
    
    /**
     * Load tất cả users từ backend ở background thread.
     */
    private void loadAllUsersAsync() {
        // Hiển thị trạng thái loading
        if (lblMessage != null) {
            lblMessage.setText("Loading users...");
            lblMessage.setStyle("-fx-text-fill: #1976d2;");
        }
        
        // Disable button khi đang load
        if (btnBan != null) {
            btnBan.setDisable(true);
        }
        
        // Fetch users ở background
        FxAsync.run("admin-load-users",
                protocol::getAllUsers,
                users -> {
                    if (tableUsers != null) {
                        ObservableList<User> data = FXCollections.observableArrayList(users);
                        tableUsers.setItems(data);
                    }
                    
                    if (lblMessage != null) {
                        lblMessage.setText("Loaded " + users.size() + " users");
                        lblMessage.setStyle("-fx-text-fill: #2e7d32;");
                    }
                    
                    if (btnBan != null) {
                        btnBan.setDisable(false);
                    }
                },
                error -> {
                    System.err.println("Error loading users: " + error);
                    
                    if (lblMessage != null) {
                        lblMessage.setText("Could not connect to server");
                        lblMessage.setStyle("-fx-text-fill: #c62828;");
                    }
                    
                    if (btnBan != null) {
                        btnBan.setDisable(false);
                    }
                });
    }
    
    /**
     * Ban selected user.
     */
    @FXML
    private void handleBanUser() {
        if (tableUsers == null || lblMessage == null) {
            return;
        }
        
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblMessage.setText("Select a user to ban");
            lblMessage.setStyle("-fx-text-fill: #f57c00;");
            return;
        }
        
        // Confirm dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm");
        confirm.setHeaderText("Ban user: " + selected.getUsername());
        confirm.setContentText("Are you sure you want to ban this user?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        // Disable button để tránh double-click
        if (btnBan != null) {
            btnBan.setDisable(true);
        }
        
        lblMessage.setText("Banning user...");
        lblMessage.setStyle("-fx-text-fill: #1976d2;");
        
        // Gọi backend ở background thread
        FxAsync.run("admin-ban-user",
                () -> protocol.banUser(selected.getId()),
                success -> {
                    if (success) {
                        lblMessage.setText("Banned user: " + selected.getUsername());
                        lblMessage.setStyle("-fx-text-fill: #2e7d32;");
                        
                        // Reload table
                        loadAllUsersAsync();
                    } else {
                        lblMessage.setText("Could not ban user (server error)");
                        lblMessage.setStyle("-fx-text-fill: #c62828;");
                        
                        if (btnBan != null) {
                            btnBan.setDisable(false);
                        }
                    }
                },
                error -> {
                    lblMessage.setText("Error: " + error);
                    lblMessage.setStyle("-fx-text-fill: #c62828;");
                    
                    if (btnBan != null) {
                        btnBan.setDisable(false);
                    }
                });
    }
    
    /**
     * Refresh table.
     */
    @FXML
    private void handleRefresh() {
        loadAllUsersAsync();
    }
    
    // ==================== Navigation ====================
    
    @FXML
    public void switchHome(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.ADMIN_DASHBOARD, "admin dashboard");
    }
    
    @FXML
    public void switchCategory(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.C_MANAGEMENT, "category dashboard");
    }
    
    @FXML
    public void switchLogout(MouseEvent mouseEvent) {
        SceneNavigator.loadScene(SceneNavigator.LOGIN, "login dashboard");
    }
}
