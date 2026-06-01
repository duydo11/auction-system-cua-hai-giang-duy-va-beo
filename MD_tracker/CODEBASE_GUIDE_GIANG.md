# 📖 Hướng dẫn Codebase cho GIANG — Frontend JavaFX & UI

> **Vai trò của Giang:** Frontend JavaFX — FXML, Controllers, Data Binding, Realtime UI Updates  
> **Phạm vi chính:** `client/src/main/resources/fxml/` + `client/src/main/java/com/auction/client/controller/`

---

## 🗺️ Các file của Giang trong hệ thống

```
auction_system/
├── client/src/main/resources/
│   ├── fxml/                    ← FXML files (UI layout)
│   │   ├── ActionsScene/
│   │   │   ├── AuctionDetailsforBidder.fxml
│   │   │   ├── AuctionDetailsforSeller.fxml
│   │   │   ├── DepositAction.fxml
│   │   │   └── WithdrawAction.fxml
│   │   ├── BidderScene/
│   │   │   ├── BidderDashboard.fxml
│   │   │   └── Wallet1.fxml
│   │   ├── SellerScene/
│   │   │   ├── SellerDashboard.fxml
│   │   │   └── Wallet2.fxml
│   │   ├── Card/
│   │   │   ├── ProductCard.fxml
│   │   │   ├── BidderHistoryCard.fxml
│   │   │   ├── TransHisCard.fxml
│   │   │   ├── AuctionResult1Card.fxml
│   │   │   └── AuctionResult2Card.fxml
│   │   ├── Login.fxml
│   │   └── Register.fxml
│   └── png/                     ← Images/icons
│
└── client/src/main/java/com/auction/client/
    ├── controller/              ← Java Controllers (logic)
    │   ├── ActionsScene/
    │   ├── BidderScene/
    │   ├── SellerScene/
    │   ├── Card/
    │   ├── LoginController.java
    │   └── RegisterController.java
    ├── network/
    │   ├── ClientProtocolHandler.java  ← API calls
    │   ├── SocketClient.java           ← Network layer
    │   └── ClientConnection.java       ← Connection manager
    ├── util/
    │   ├── SessionContext.java         ← Current user state
    │   └── AuctionCache.java           ← Local cache
    └── RealtimeAuctionBus.java         ← Realtime updates
```

---

## 🏗️ Kiến trúc Frontend (FXML + Controller + Protocol)

### Luồng dữ liệu hoàn chỉnh:

```
┌─────────────────────────────────────────────────────────────┐
│                    USER INTERACTION                          │
│  User clicks button / enters text / selects item            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    FXML (UI Layout)                          │
│  - Defines visual structure (buttons, labels, text fields)  │
│  - fx:id links to Controller fields                         │
│  - onAction/onMouseClicked links to Controller methods      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 CONTROLLER (Java Logic)                      │
│  - @FXML fields (lblUsername, txtBidAmount, etc.)          │
│  - Event handlers (handlePlaceBid, handleLogin, etc.)      │
│  - Data binding (set text, update UI)                      │
│  - Calls ClientProtocolHandler for backend operations      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│            ClientProtocolHandler (API Layer)                 │
│  - login(username, password) → Message                      │
│  - getActiveAuctions() → List<AuctionSession>              │
│  - placeBid(sessionId, bidderId, amount) → boolean         │
│  - Wraps SocketClient.sendMessage()                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              SocketClient (Network Layer)                    │
│  - TCP Socket connection to server                          │
│  - Serializes/deserializes Message objects                  │
│  - RPC: correlationId matching                              │
│  - Push: forwards to RealtimeAuctionBus                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
                    [SERVER]
```

### Realtime Update Flow (Server → Client):

```
[SERVER] Bid placed
    ↓
ClientBroadcastHub.broadcast(AUCTION_UPDATED_PUSH)
    ↓
SocketClient.dispatchIncoming()
    ↓
RealtimeAuctionBus.dispatch()
    ↓
All registered listeners (Controllers)
    ↓
Platform.runLater(() -> updateUI())
```

---

## 📋 Controller Lifecycle

### 1. Controller Creation & Initialization

```java
public class BidderDashboardController implements Initializable {
    // Step 1: FXML Injection (happens automatically)
    @FXML private Label lblUsername;
    @FXML private HBox container1;
    @FXML private HBox container2;
    
    // Step 2: Dependencies
    private ClientProtocolHandler protocol = new ClientProtocolHandler();
    private Consumer<AuctionSession> realtimeListener;
    
    // Step 3: Initialize (called after FXML injection)
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load current user
        User currentUser = SessionContext.getCurrentUser();
        lblUsername.setText(currentUser.getUsername());
        
        // Load data from backend
        loadActiveAuctions();
        
        // Setup realtime listener
        setupRealtimeListener();
    }
}
```

**Thứ tự thực thi:**
1. JavaFX loads FXML file
2. JavaFX creates Controller instance
3. JavaFX injects `@FXML` fields (links fx:id to Java fields)
4. JavaFX calls `initialize()` method
5. Controller loads data and sets up listeners

---

## 🔗 FXML ↔ Controller Connection

### FXML File Structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.auction.client.controller.BidderScene.BidderDashboardController">
    
    <!-- fx:id links to @FXML field in Controller -->
    <Label fx:id="lblUsername" text="Username" />
    
    <!-- onAction links to @FXML method in Controller -->
    <Button text="Place Bid" onAction="#handlePlaceBid" />
    
    <!-- onMouseClicked for HBox/VBox (not Button) -->
    <HBox fx:id="btnConfirm" onMouseClicked="#handleConfirm">
        <Label text="Confirm" />
    </HBox>
</VBox>
```

### Controller Mapping:

```java
public class BidderDashboardController {
    // Field name MUST match fx:id
    @FXML private Label lblUsername;
    
    // Method name MUST match onAction (without #)
    @FXML
    private void handlePlaceBid(ActionEvent event) {
        // Handle button click
    }
    
    // Method name MUST match onMouseClicked (without #)
    @FXML
    private void handleConfirm(MouseEvent event) {
        // Handle HBox click
    }
}
```

**⚠️ Quan trọng:**
- `fx:id` PHẢI khớp với tên field trong Controller
- `onAction` PHẢI khớp với tên method trong Controller (thêm `#` ở FXML)
- Nếu không khớp → `NullPointerException` hoặc `LoadException`

---

## 📊 Data Binding Patterns

### Pattern 1: Simple Label Binding

```java
// Model
AuctionSession session = protocol.getSessionById(1);

// Binding
lblItemName.setText(session.getItem().getName());
lblCurrentPrice.setText(String.format("$%,.2f", session.getCurrentPrice()));
lblStatus.setText(session.getStatus().toString());
```

### Pattern 2: Dynamic Card Loading

```java
private void loadActiveAuctions() {
    // 1. Get data from backend
    List<AuctionSession> auctions = protocol.getActiveAuctions();
    
    // 2. Clear existing cards
    container1.getChildren().clear();
    
    // 3. Load each auction as a card
    for (AuctionSession session : auctions) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Card/ProductCard.fxml")
            );
            Node card = loader.load();
            
            // Get controller and set data
            ProductCardController controller = loader.getController();
            controller.setAuctionSession(session);
            
            // Add to container
            container1.getChildren().add(card);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Pattern 3: TableView Binding

```java
@FXML private TableView<User> tableUsers;
@FXML private TableColumn<User, Integer> colId;
@FXML private TableColumn<User, String> colUsername;

private void loadUsers() {
    // Setup columns
    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    
    // Load data
    List<User> users = protocol.getAllUsers();
    ObservableList<User> data = FXCollections.observableArrayList(users);
    tableUsers.setItems(data);
}
```

### Pattern 4: LineChart Binding (Bid History)

```java
@FXML private LineChart<String, Number> lcPriceHistory;

private void loadBidHistoryChart(int sessionId) {
    // Get bid history
    List<Bid> bids = protocol.getBidHistory(sessionId);
    
    // Create series
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName("Bid History");
    
    // Add starting price
    series.getData().add(new XYChart.Data<>("Start", session.getStartingPrice()));
    
    // Add each bid
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    for (Bid bid : bids) {
        String timeLabel = bid.getTime().format(formatter);
        series.getData().add(new XYChart.Data<>(timeLabel, bid.getAmount()));
    }
    
    // Add to chart
    lcPriceHistory.getData().clear();
    lcPriceHistory.getData().add(series);
}
```

---

## 🌐 API Calls (ClientProtocolHandler)

### Available API Methods:

```java
ClientProtocolHandler protocol = new ClientProtocolHandler();

// Authentication
User user = protocol.login(username, password);
boolean success = protocol.register(username, password, email, role);

// Auction Operations
List<AuctionSession> auctions = protocol.getActiveAuctions();
AuctionSession session = protocol.getSessionById(sessionId);
boolean created = protocol.createAuctionOrError(session);

// Bidding
boolean bidPlaced = protocol.placeBid(sessionId, bidderId, amount);
List<Bid> history = protocol.getBidHistory(sessionId);

// Auto-bid
boolean registered = protocol.registerAutoBid(config);
boolean cancelled = protocol.cancelAutoBid(sessionId, bidderId);

// Wallet
boolean deposited = protocol.deposit(userId, amount);
boolean withdrawn = protocol.withdraw(userId, amount);
WalletData wallet = protocol.getWalletData(userId);

// Admin
List<User> users = protocol.getAllUsers();
boolean banned = protocol.banUser(userId);
```

### API Call Example with Error Handling:

```java
@FXML
private void handlePlaceBid() {
    try {
        // 1. Validate input
        String amountText = txtBidAmount.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            showError("Please enter bid amount");
            return;
        }
        
        double amount = Double.parseDouble(amountText);
        if (amount <= session.getCurrentPrice()) {
            showError("Bid must be higher than current price");
            return;
        }
        
        // 2. Get current user
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) {
            showError("Please login first");
            return;
        }
        
        // 3. Call backend
        boolean success = protocol.placeBid(
            session.getId(),
            currentUser.getId(),
            amount
        );
        
        // 4. Handle result
        if (success) {
            showSuccess("Bid placed successfully!");
            txtBidAmount.clear();
            // UI will auto-update via realtime push
        } else {
            String error = protocol.getLastError();
            showError(error != null ? error : "Failed to place bid");
        }
        
    } catch (NumberFormatException e) {
        showError("Invalid amount format");
    } catch (Exception e) {
        showError("Error: " + e.getMessage());
    }
}
```

---

## ⚡ Realtime Update Mechanism

### RealtimeAuctionBus Architecture:

```java
// File: client/src/main/java/com/auction/client/RealtimeAuctionBus.java

public class RealtimeAuctionBus {
    // Thread-safe list of listeners
    private static final List<Consumer<AuctionSession>> listeners = 
        new CopyOnWriteArrayList<>();
    
    // Register listener (called by Controllers)
    public static void addAuctionListener(Consumer<AuctionSession> listener) {
        listeners.add(listener);
    }
    
    // Unregister listener (called when Controller is destroyed)
    public static void removeAuctionListener(Consumer<AuctionSession> listener) {
        listeners.remove(listener);
    }
    
    // Dispatch push message (called by SocketClient)
    public static void dispatch(Message message) {
        Object data = message.getData();
        if (!(data instanceof AuctionSession)) return;
        
        AuctionSession session = (AuctionSession) data;
        
        // MUST run on JavaFX Application Thread
        Platform.runLater(() -> {
            for (Consumer<AuctionSession> listener : listeners) {
                try {
                    listener.accept(session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
```

### Controller Setup Realtime Listener:

```java
public class BidderDashboardController {
    private Consumer<AuctionSession> realtimeListener;
    private Map<Integer, ProductCardController> cardControllers = new HashMap<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadActiveAuctions();
        setupRealtimeListener();
    }
    
    private void setupRealtimeListener() {
        // Create listener
        realtimeListener = updatedSession -> {
            // This runs on JavaFX thread (Platform.runLater already called)
            
            // Update cache
            AuctionCache.addOrReplace(updatedSession);
            
            // Find card for this session
            ProductCardController card = cardControllers.get(updatedSession.getId());
            if (card != null) {
                // Update card immediately
                card.setAuctionSession(updatedSession);
            }
            
            // Optionally reload full list
            // loadActiveAuctions();
        };
        
        // Register listener
        RealtimeAuctionBus.addAuctionListener(realtimeListener);
    }
    
    // IMPORTANT: Cleanup when controller is destroyed
    public void cleanup() {
        if (realtimeListener != null) {
            RealtimeAuctionBus.removeAuctionListener(realtimeListener);
        }
    }
}
```

---

## ⏱️ Common Controller Patterns

### Pattern 1: Countdown Timer

```java
private Timeline countdownTimer;

private void startCountdown(LocalDateTime endTime) {
    // Stop existing timer
    if (countdownTimer != null) {
        countdownTimer.stop();
    }
    
    // Create new timer (update every second)
    countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
        updateTimeRemaining(endTime);
    }));
    countdownTimer.setCycleCount(Timeline.INDEFINITE);
    countdownTimer.play();
}

private void updateTimeRemaining(LocalDateTime endTime) {
    LocalDateTime now = LocalDateTime.now();
    long seconds = ChronoUnit.SECONDS.between(now, endTime);
    
    if (seconds <= 0) {
        lblTimeH.setText("00");
        lblTimem.setText("00");
        lblTimes.setText("00");
        lblStatus.setText("ENDED");
        countdownTimer.stop();
        return;
    }
    
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    
    lblTimeH.setText(String.format("%02d", hours));
    lblTimem.setText(String.format("%02d", minutes));
    lblTimes.setText(String.format("%02d", secs));
    
    // Warning when < 5 minutes
    if (seconds < 300) {
        lblWarning.setText("⚠️ Auction ending soon!");
        lblWarning.setStyle("-fx-text-fill: #b45309;");
    }
}
```

### Pattern 2: Input Validation

```java
private boolean validateBidAmount(String amountText) {
    // Check empty
    if (amountText == null || amountText.trim().isEmpty()) {
        showError("Please enter bid amount");
        return false;
    }
    
    // Check numeric
    try {
        double amount = Double.parseDouble(amountText);
        
        // Check positive
        if (amount <= 0) {
            showError("Amount must be positive");
            return false;
        }
        
        // Check higher than current price
        if (amount <= session.getCurrentPrice()) {
            showError("Bid must be higher than $" + 
                String.format("%.2f", session.getCurrentPrice()));
            return false;
        }
        
        return true;
        
    } catch (NumberFormatException e) {
        showError("Invalid number format");
        return false;
    }
}
```

### Pattern 3: Scene Navigation

```java
// File: client/util/SceneNavigator.java (utility class)

public class SceneNavigator {
    public static void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                SceneNavigator.class.getResource(fxmlPath)
            );
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Usage in Controller:
@FXML
private void handleViewDetails(ActionEvent event) {
    SceneNavigator.navigateTo("/fxml/ActionsScene/AuctionDetailsforBidder.fxml", event);
}
```

---

## 🎯 Specific Controller Examples

### Example 1: ProductCardController

```java
public class ProductCardController {
    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStatus;
    @FXML private Button btnViewDetails;
    
    private AuctionSession session;
    
    public void setAuctionSession(AuctionSession session) {
        this.session = session;
        
        // Bind data
        lblItemName.setText(session.getItem().getName());
        lblCurrentPrice.setText(String.format("$%,.2f", session.getCurrentPrice()));
        
        // Set status with color
        AuctionStatus status = session.getStatus();
        lblStatus.setText(status.toString());
        switch (status) {
            case OPEN -> lblStatus.setStyle("-fx-text-fill: #fbbf24;");
            case RUNNING -> lblStatus.setStyle("-fx-text-fill: #10b981;");
            case FINISHED -> lblStatus.setStyle("-fx-text-fill: #ef4444;");
            case CANCELED -> lblStatus.setStyle("-fx-text-fill: #6b7280;");
        }
    }
    
    @FXML
    private void handleViewDetails() {
        // Store session in context for detail page
        SessionContext.setCurrentSession(session);
        
        // Navigate to detail page
        // (navigation code here)
    }
}
```

### Example 2: Wallet Controller

```java
public class Wallet1Controller {
    @FXML private Label lblTotalBalance;
    @FXML private Label lblAvailable;
    @FXML private Label lblReserved;
    @FXML private VBox containerTrans;
    
    private ClientProtocolHandler protocol = new ClientProtocolHandler();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadWalletData();
    }
    
    private void loadWalletData() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) return;
        
        // Get wallet data
        WalletData data = protocol.getWalletData(currentUser.getId());
        
        // Bind balance
        lblTotalBalance.setText(String.format("$%,.2f", data.totalBalance()));
        lblAvailable.setText(String.format("$%,.2f", data.available()));
        lblReserved.setText(String.format("$%,.2f", data.reserved()));
        
        // Load transaction history
        containerTrans.getChildren().clear();
        for (Transaction trans : data.transactions()) {
            loadTransactionCard(trans);
        }
    }
    
    private void loadTransactionCard(Transaction trans) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Card/TransHisCard.fxml")
            );
            Node card = loader.load();
            
            TransHisCardController controller = loader.getController();
            controller.setTransaction(trans);
            
            containerTrans.getChildren().add(card);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

---

## ❓ FAQ cho Giang

**Q: Tại sao `@FXML` field bị `NullPointerException`?**  
A: Thường do `fx:id` trong FXML không khớp với tên field trong Controller. Check lại chính tả.

**Q: Tại sao click button không chạy?**  
A: Check `onAction="#methodName"` trong FXML phải khớp với `@FXML private void methodName()` trong Controller.

**Q: Tại sao realtime không update?**  
A: Check:
1. Server đang chạy
2. Client còn connected
3. Controller đã register listener với `RealtimeAuctionBus`
4. Listener chưa bị remove

**Q: Tại sao chart không hiện?**  
A: Check:
1. `fx:id="lcPriceHistory"` đúng
2. Auction có bid history
3. Chart được add vào scene graph

**Q: Làm sao để debug UI?**  
A: Thêm `System.out.println()` trong:
- `initialize()` - check có được gọi không
- Event handlers - check có được trigger không
- Data binding - check data có null không

---

## 🎓 Best Practices cho Giang

1. **Luôn validate input** trước khi gọi backend
2. **Luôn handle errors** và show message cho user
3. **Luôn cleanup listeners** khi controller bị destroy
4. **Luôn dùng Platform.runLater()** khi update UI từ background thread
5. **Không block UI thread** - dùng Task/Service cho long operations
6. **Keep controllers focused** - mỗi controller một responsibility
7. **Reuse components** - tạo card/dialog reusable
8. **Test với real data** - không chỉ mock data

---

**Last Updated:** 2026-06-01  
**Maintained by:** Giang (Frontend Lead)
