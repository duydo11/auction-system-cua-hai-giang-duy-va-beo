# 🎨 Hướng dẫn cho Giang — FXML Changes Needed

## ✅ Đã hoàn thành (Backend Integration)

Tôi đã implement **data binding** cho 4 controllers chính:

| Controller | Status | Chức năng |
|------------|--------|-----------|
| `ProductCardController` | ✅ Done | Bind AuctionSession → card, countdown timer, realtime update |
| `BidderDashboardController` | ✅ Done | Load real auctions, listen realtime push |
| `HistoryCardController` | ✅ Done | Bind Bid → card |
| `SellerDashboardController` | ✅ Done | Create auction với real backend call |
| `UserManagementController` | ✅ Done | TableView users, ban user |

---

## 🎨 Giang cần làm: Thêm fx:id vào FXML

### 1. ProductCard.fxml

**File**: `client/src/main/resources/fxml/Card/ProductCard.fxml`

**Cần thêm fx:id cho các Label/Button:**

```xml
<!-- Line 31: Product name label -->
<Label fx:id="lblItemName" prefHeight="30.0" prefWidth="219.0" text="Product name (Details)">

<!-- Line 59: Current price label -->
<Label fx:id="lblCurrentPrice" prefHeight="22.0" prefWidth="122.0" text="000.000.000.0">

<!-- Line 19: Status badge -->
<Label fx:id="lblStatus" alignment="CENTER" layoutX="191.0" layoutY="3.0" 
       prefHeight="22.0" prefWidth="51.0" text="STATUS">

<!-- Line 64: View Details button -->
<Button fx:id="btnViewDetails" mnemonicParsing="false" 
        onAction="#handleViewDetails" 
        prefHeight="23.0" prefWidth="91.0" text="View Details">
```

**Thêm Label mới cho time remaining** (đặt ở đâu đó trong card):
```xml
<Label fx:id="lblTimeRemaining" text="--h --m" 
       style="-fx-font-size: 12px; -fx-text-fill: #424242;">
    <font>
        <Font name="Montserrat Medium" size="12.0" />
    </font>
</Label>
```

---

### 2. HistoryCard.fxml (nếu có)

**File**: `client/src/main/resources/fxml/Card/HistoryCard.fxml`

**Cần thêm fx:id:**

```xml
<Label fx:id="lblBidAmount" text="000.000 VND">
<Label fx:id="lblBidTime" text="HH:mm dd/MM">
<Label fx:id="lblBidder" text="Username">
<Label fx:id="lblAuctionId" text="Phiên #123">
```

---

### 3. UserManagement.fxml

**File**: `client/src/main/resources/fxml/AdminScene/UserManagement.fxml`

**Cần thêm TableView:**

```xml
<TableView fx:id="tableUsers" prefHeight="400.0" prefWidth="600.0">
    <columns>
        <TableColumn fx:id="colId" text="ID" prefWidth="50.0" />
        <TableColumn fx:id="colUsername" text="Username" prefWidth="150.0" />
        <TableColumn fx:id="colEmail" text="Email" prefWidth="200.0" />
        <TableColumn fx:id="colRole" text="Role" prefWidth="100.0" />
    </columns>
</TableView>

<Button fx:id="btnBan" text="Ban User" onAction="#handleBanUser" 
        style="-fx-background-color: #c62828; -fx-text-fill: white;">
    <font>
        <Font name="Montserrat Bold" size="12.0" />
    </font>
</Button>

<Button text="Refresh" onAction="#handleRefresh" 
        style="-fx-background-color: #1976d2; -fx-text-fill: white;">
    <font>
        <Font name="Montserrat Bold" size="12.0" />
    </font>
</Button>

<Label fx:id="lblMessage" text="" 
       style="-fx-font-size: 14px;">
    <font>
        <Font name="Montserrat Medium" size="14.0" />
    </font>
</Label>
```

---

### 4. SellerDashboard.fxml

**File**: `client/src/main/resources/fxml/SellerScene/SellerDashboard.fxml`

**Kiểm tra các fx:id đã có chưa:**

```xml
<TextField fx:id="txtItemName" promptText="Tên sản phẩm">
<TextField fx:id="txtItemDesc" promptText="Mô tả">
<TextField fx:id="txtStartPrice" promptText="Giá khởi điểm">
<TextField fx:id="txtDurationHours" promptText="Số giờ">
<Label fx:id="lblCreateAuctionMsg" text="">
<Button text="Tạo phiên" onAction="#handleCreateAuction">
```

---

## 📊 Tổng kết thay đổi

### Backend Integration (✅ Done by Duy)

| File | Changes |
|------|---------|
| `ProductCardController.java` | ✅ Data binding, countdown timer, realtime update |
| `BidderDashboardController.java` | ✅ Load real auctions, realtime listener, cleanup |
| `HistoryCardController.java` | ✅ Bind Bid data |
| `SellerDashboardController.java` | ✅ Enabled `handleCreateAuction` |
| `UserManagementController.java` | ✅ TableView, ban user, refresh |

### UI Changes (⚠️ Giang cần làm)

| File | Action |
|------|--------|
| `ProductCard.fxml` | ⚠️ Thêm fx:id: `lblItemName`, `lblCurrentPrice`, `lblStatus`, `lblTimeRemaining`, `btnViewDetails` |
| `HistoryCard.fxml` | ⚠️ Thêm fx:id: `lblBidAmount`, `lblBidTime`, `lblBidder`, `lblAuctionId` |
| `UserManagement.fxml` | ⚠️ Thêm TableView + columns + buttons |
| `SellerDashboard.fxml` | ⚠️ Kiểm tra fx:id đã đủ chưa |

---

## 🎯 Cách test

### Test ProductCard
1. Chạy server: `ServerMain.java`
2. Chạy client: `MainApp.java`
3. Login as Bidder
4. Vào BidderDashboard → sẽ thấy cards với data thật
5. Kiểm tra:
   - ✅ Tên sản phẩm hiển thị
   - ✅ Giá hiện tại hiển thị
   - ✅ Countdown timer chạy (update mỗi giây)
   - ✅ Status badge đúng màu
   - ✅ Nếu < 5 phút: hiển thị đỏ ⚠️

### Test SellerDashboard
1. Login as Seller
2. Điền form: tên, mô tả, giá, số giờ
3. Click "Tạo phiên"
4. Kiểm tra:
   - ✅ Hiển thị "Đã tạo phiên thành công!" (màu xanh)
   - ✅ Form clear sau khi tạo
   - ✅ Bidder dashboard thấy phiên mới ngay lập tức

### Test UserManagement
1. Login as Admin
2. Vào User Management
3. Kiểm tra:
   - ✅ TableView hiển thị danh sách users
   - ✅ Chọn user → click "Ban User"
   - ✅ Confirm dialog xuất hiện
   - ✅ Sau khi ban: table refresh, message hiển thị

---

## 🚀 Realtime Updates

**Đã implement** — không cần làm gì thêm:

- ✅ `RealtimeAuctionBus` lắng nghe push từ server
- ✅ Khi có bid mới → tất cả ProductCard tự update giá
- ✅ Khi anti-snipe trigger → time remaining tự update
- ✅ Cleanup listeners khi đổi scene

---

## 📝 Notes cho Giang

### ProductCard Layout Suggestions

Để countdown timer dễ nhìn, đề xuất layout:

```
┌─────────────────────────────┐
│  [Image]          [STATUS]  │
│                              │
│  Product Name                │
│  💰 000.000 VND   ⏰ 2h 30m  │
│                              │
│  [View Details]              │
└─────────────────────────────┘
```

### Color Scheme

| Element | Color | Style |
|---------|-------|-------|
| Status RUNNING | Green | `#e8f5e9` bg, `#388e3c` text |
| Status FINISHED | Pink | `#fce4ec` bg, `#c2185b` text |
| Time < 5 min | Red | `#c62828` text, bold |
| Success message | Green | `#2e7d32` text |
| Error message | Red | `#c62828` text |

### Font Recommendations

Đang dùng **Montserrat** — giữ nguyên:
- Bold: titles, buttons
- Medium: body text
- Size: 10-14px

---

## ❓ Nếu gặp lỗi

### Lỗi: "fx:id not found"
→ Kiểm tra tên fx:id trong FXML khớp với `@FXML private Label lblItemName;` trong Controller

### Lỗi: "NullPointerException"
→ Kiểm tra fx:id đã thêm vào FXML chưa

### Lỗi: "Cannot load FXML"
→ Kiểm tra path: `/fxml/Card/ProductCard.fxml` (phải có `/` đầu)

### Cards không hiển thị data
→ Kiểm tra server đang chạy, có auctions trong DB chưa

---

## 🎉 Kết quả mong đợi

Sau khi Giang thêm fx:id vào FXML:

✅ **BidderDashboard**: Hiển thị cards với data thật, countdown timer chạy  
✅ **SellerDashboard**: Tạo phiên thành công, form clear  
✅ **UserManagement**: TableView hiển thị users, ban user hoạt động  
✅ **Realtime**: Giá tự update khi có bid mới  
✅ **Anti-snipe**: Time warning < 5 phút, auto-extend khi bid cuối  

**Điểm dự tính sau khi hoàn thành: 10.5/11** (chỉ thiếu Bid History Chart)

