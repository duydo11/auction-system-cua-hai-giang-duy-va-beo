package com.auction.client.controller.AdminScene;

import com.auction.client.network.ClientProtocolHandler;
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
 * UserManagement — hiển thị danh sách user, ban/unban.
 * Giang cần thêm TableView vào FXML:
 * - tableUsers (TableView<User>)
 * - colId (TableColumn<User, Integer>)
 * - colUsername (TableColumn<User, String>)
 * - colEmail (TableColumn<User, String>)
 * - colRole (TableColumn<User, String>)
 * - btnBan (Button)
 * - lblMessage (Label)
 */
public class UserManagementController implements Initializable {
    
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private Button btnBan;
    @FXML private Label lblMessage;
    
    private final ClientProtocolHandler protocol = new ClientProtocolHandler();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadAllUsers();
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
        
        if (colRole != null) {
            colRole.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getRoleName())
            );
        }
    }
    
    /**
     * Load all users từ backend.
     */
    private void loadAllUsers() {
        try {
            List<User> users = protocol.getAllUsers();
            
            if (tableUsers != null) {
                ObservableList<User> data = FXCollections.observableArrayList(users);
                tableUsers.setItems(data);
            }
            
            if (lblMessage != null) {
                lblMessage.setText("Đã tải " + users.size() + " users");
                lblMessage.setStyle("-fx-text-fill: #2e7d32;");
            }
            
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
            
            if (lblMessage != null) {
                lblMessage.setText("Không thể kết nối server");
                lblMessage.setStyle("-fx-text-fill: #c62828;");
            }
        }
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
            lblMessage.setText("Chọn user cần ban");
            lblMessage.setStyle("-fx-text-fill: #f57c00;");
            return;
        }
        
        // Confirm dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Ban user: " + selected.getUsername());
        confirm.setContentText("Bạn có chắc muốn ban user này?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        // Call backend
        boolean success = protocol.banUser(selected.getId());
        
        if (success) {
            lblMessage.setText("Đã ban user: " + selected.getUsername());
            lblMessage.setStyle("-fx-text-fill: #2e7d32;");
            
            // Reload table
            loadAllUsers();
        } else {
            lblMessage.setText("Không thể ban user (lỗi server)");
            lblMessage.setStyle("-fx-text-fill: #c62828;");
        }
    }
    
    /**
     * Refresh table.
     */
    @FXML
    private void handleRefresh() {
        loadAllUsers();
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
