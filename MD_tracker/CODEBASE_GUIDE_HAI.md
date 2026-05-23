# 📖 Hướng dẫn Codebase cho HẢI — Domain Model & Business Logic

> **Vai trò của Hải:** Thiết kế domain model, business rules, custom exceptions, AuctionSession lifecycle.  
> **Phạm vi chính:** `shared/` module + `server/service/AuctionService.java`

---

## 🗺️ Bức tranh toàn cảnh hệ thống

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT (JavaFX UI)                          │
│   Giang vẽ giao diện → User thao tác → Controller gọi protocol  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ TCP Socket (Java ObjectStream)
                           │ Gửi/nhận object Message
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                      SERVER                                       │
│   ServerMain → SocketServer → ClientHandler (mỗi client 1 thread)│
│   ClientHandler → ServerProtocolHandler → Service → DAO → MySQL  │
└─────────────────────────────────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │   DATABASE   │
                    │    MySQL     │
                    └─────────────┘
```

**Luồng đơn giản nhất** (Bidder đặt giá):
```
Bidder bấm nút "Đặt giá"
  → AuctionDetailsforBidderController.handlePlaceBid()
  → ClientProtocolHandler.placeBid(sessionId, bidderId, amount)
  → SocketClient gửi Message{type=PLACE_BID_REQUEST, data=["1","2","500000"]}
  → Server nhận → ClientHandler đọc → ServerProtocolHandler.handlePlaceBid()
  → BidService.placeBid() → AuctionSession.updateCurrentPrice()
  → BidDAO.saveBid() → MySQL INSERT INTO bids
  → ClientBroadcastHub.broadcast(AUCTION_UPDATED_PUSH) → Tất cả client update giá
```

---

## 🏗️ Module `shared` — Xương sống của hệ thống

`shared` là module dùng chung giữa client và server. Nó chứa:
- **Models** (các class dữ liệu): User, Item, AuctionSession, Bid...
- **Protocol** (giao thức giao tiếp): Message, MessageType
- **Exceptions** (lỗi nghiệp vụ): BidTooLowException, AuctionClosedException...

### 📂 Cấu trúc thư mục `shared`

```
shared/
└── src/main/java/com/auction/shared/
    ├── model/
    │   ├── Entity.java              ← Class cha của tất cả model
    │   ├── user/
    │   │   ├── User.java            ← Abstract class cha cho 3 role
    │   │   ├── Bidder.java          ← Người đấu giá
    │   │   ├── Seller.java          ← Người bán
    │   │   └── Admin.java           ← Quản trị viên
    │   ├── item/
    │   │   ├── Item.java            ← Abstract class cha cho 3 loại hàng
    │   │   ├── Electronics.java     ← Đồ điện tử
    │   │   ├── Art.java             ← Tác phẩm nghệ thuật
    │   │   ├── Vehicle.java         ← Xe cộ
    │   │   └── ItemFactory.java     ← Factory pattern tạo Item
    │   └── auction/
    │       ├── AuctionSession.java  ← Phiên đấu giá (cốt lõi nhất)
    │       ├── Bid.java             ← Một lần đặt giá
    │       ├── AuctionStatus.java   ← Enum trạng thái
    │       └── AutoBidConfig.java   ← Config auto-bid
    ├── protocol/
    │   ├── Message.java             ← Gói tin giao tiếp
    │   └── MessageType.java         ← Enum 40+ loại message
    └── exception/
        ├── AuctionException.java    ← Base exception
        ├── AuctionClosedException.java
        ├── BidTooLowException.java
        ├── InvalidDataException.java
        └── UnauthorizedRoleException.java
```

---

## 🧬 Cây kế thừa (Inheritance Tree)

### User

```
Entity (abstract, có field: int id)
  └── User (abstract, có: username, password, email)
        ├── Bidder       → role="BIDDER", thêm: accountBalance (double)
        ├── Seller       → role="SELLER", thêm: rating (double)
        └── Admin        → role="ADMIN",  thêm: accessLevel (String)
```

**Tại sao dùng abstract?**  
Vì không ai tạo `new User()` trực tiếp — luôn phải là Bidder, Seller, hoặc Admin. `abstract` buộc phải implement method `getRoleName()`.

**Code ví dụ:**
```java
// Đây là pattern đúng để tạo user mới
User user = new Bidder(1, "nguyen_van_a", "123456", "a@email.com", 0.0);
System.out.println(user.getRoleName()); // in ra "BIDDER"

// Kiểm tra role
if (user instanceof Bidder bidder) {
    System.out.println("Số dư: " + bidder.getAccountBalance());
}
if (user instanceof Seller seller) {
    System.out.println("Rating: " + seller.getRating());
}
```

**Map với Database:**
```
users table:    id, username, password, email
bidders table:  user_id → FK to users.id, account_balance
sellers table:  user_id → FK to users.id, rating
admins table:   user_id → FK to users.id, access_level
```

---

### Item

```
Entity (abstract, có field: int id)
  └── Item (abstract, có: name, description, seller)
        ├── Electronics  → thêm: warrantyMonths (int)
        ├── Art          → thêm: author (String)
        └── Vehicle      → thêm: brand (String)
```

**Map với Database:**
```
items table:        id, name, description, seller_id
electronics table:  item_id → FK to items.id, warranty_months
arts table:         item_id → FK to items.id, author
vehicles table:     item_id → FK to items.id, brand
```

**Dùng ItemFactory để tạo:**
```java
// Factory Method Pattern — tạo item theo loại
Item laptop = ItemFactory.create("electronics", 0, "MacBook Pro", "Laptop Apple", seller, 24);
Item painting = ItemFactory.create("art", 0, "Mona Lisa", "Kiệt tác", seller, "Da Vinci");
Item car = ItemFactory.create("vehicle", 0, "Toyota Camry", "Sedan", seller, "Toyota");
```

---

## 🎯 AuctionSession — Trung tâm của nghiệp vụ

Đây là class quan trọng nhất. Mỗi `AuctionSession` đại diện cho **một phiên đấu giá** với đầy đủ thông tin.

### Các field:
```java
public class AuctionSession extends Entity {
    private User seller;          // Người tạo phiên (Seller)
    private User winner;          // Người thắng (null nếu chưa có bid)
    private Item item;            // Sản phẩm đấu giá
    private double startingPrice; // Giá khởi điểm (không đổi)
    private double currentPrice;  // Giá hiện tại (tăng theo bid)
    private LocalDateTime startTime;  // Thời gian bắt đầu
    private LocalDateTime endTime;    // Thời gian kết thúc
    private List<Bid> bids;       // Lịch sử tất cả bid
    private AuctionStatus status; // Trạng thái hiện tại
}
```

### Vòng đời (State Machine):

```
Tạo phiên → OPEN
    ↓ (có bid đầu tiên)
  RUNNING
    ↓ (hết giờ, AuctionScheduler tự đóng)
  FINISHED
    ↓ (tùy nghiệp vụ)
  PAID hoặc CANCELED
```

**Code trạng thái:**
```java
public enum AuctionStatus {
    OPEN,      // Vừa tạo, chưa có bid nào
    RUNNING,   // Đang diễn ra, đã có bid
    FINISHED,  // Hết giờ
    PAID,      // Đã thanh toán (hiện chưa implement)
    CANCELED   // Bị hủy
}
```

### Method `updateCurrentPrice(Bid bid)` — Nghiệp vụ cốt lõi:

```java
public void updateCurrentPrice(Bid bid) {
    if (this.isActive()) {
        // Điều kiện bid hợp lệ:
        // 1. Giá bid > giá hiện tại
        // 2. Bid được đặt sau startTime
        // 3. Bid được đặt trước endTime
        if (bid.getAmount() > this.currentPrice
                && this.startTime.isBefore(bid.getTime())
                && this.endTime.isAfter(bid.getTime())) {
            
            this.currentPrice = bid.getAmount(); // Cập nhật giá
            this.winner = bid.getBidder();        // Cập nhật người thắng tạm thời
            bids.add(bid);                        // Ghi vào lịch sử
            
            // Chuyển OPEN → RUNNING khi có bid đầu tiên
            if (this.status == AuctionStatus.OPEN) {
                this.status = AuctionStatus.RUNNING;
            }
        }
    }
}
```

### Method `isActive()` — Kiểm tra phiên có thể bid không:

```java
public boolean isActive() {
    LocalDateTime now = LocalDateTime.now();
    boolean timeValid = now.isAfter(startTime) && now.isBefore(endTime);
    return timeValid && (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING);
}
```

---

## 🔴 Custom Exceptions — Xử lý lỗi nghiệp vụ

Tất cả exception kế thừa từ `AuctionException` (extends `RuntimeException`):

```
AuctionException (base)
  ├── AuctionClosedException  → Bid vào phiên đã đóng
  ├── BidTooLowException      → Giá bid < giá hiện tại
  ├── InvalidDataException    → Dữ liệu đầu vào sai (tên trống, giá âm...)
  └── UnauthorizedRoleException → User sai role thực hiện hành động
```

**Cách dùng:**
```java
// Trong BidService (Duy viết)
if (!session.isActive()) {
    throw new AuctionClosedException(sessionId);
    // Message: "Phiên đấu giá #1 đã đóng"
}

if (amount <= session.getCurrentPrice()) {
    throw new BidTooLowException(amount, session.getCurrentPrice());
    // Message: "Bid 500.00 quá thấp. Giá hiện tại: 1000.00"
}
```

---

## 📦 Design Patterns Hải đã áp dụng

### 1. Factory Method — `ItemFactory.java`

**Vấn đề:** Khi tạo Item, không biết trước loại nào (Electronics/Art/Vehicle).  
**Giải pháp:** Dùng Factory để tạo đúng subclass dựa trên string type.

```java
// Thay vì:
if (type.equals("electronics")) item = new Electronics(...);
else if (type.equals("art")) item = new Art(...);
// ... (code lặp, khó mở rộng)

// Dùng Factory:
Item item = ItemFactory.create(type, id, name, desc, seller, extraParam);
// Dễ thêm loại mới, code gọn
```

### 2. Template Method (qua Inheritance)

`updateCurrentPrice()` trong `AuctionSession` định nghĩa "template" cho việc kiểm tra và cập nhật bid. Subclass không cần override — logic tập trung một chỗ.

### 3. State Pattern — `AuctionStatus`

`AuctionSession` có trạng thái (`status`) và hành vi thay đổi theo trạng thái. Ví dụ: `isActive()` trả về khác nhau tùy vào `status`.

---

## 🛠️ AuctionService.java — Service của Hải

File: `server/src/main/java/com/auction/server/service/AuctionService.java`

```java
public class AuctionService {
    // Phương thức Hải chịu trách nhiệm:
    
    // 1. Lấy danh sách phiên đang active
    public List<AuctionSession> getActiveAuctions() { ... }
    
    // 2. Lấy một phiên theo ID
    public AuctionSession getSessionById(int sessionId) { ... }
    
    // 3. Tạo phiên mới (cấp ID tự động nếu id=0)
    public boolean createAuction(AuctionSession session) { ... }
    
    // 4. Sửa item (validate: không sửa khi phiên đang RUNNING)
    public boolean updateItem(Item item) { ... }
    
    // 5. Xóa item (validate: không xóa khi phiên đang RUNNING)
    public boolean deleteItem(int itemId) { ... }
    
    // 6. Đóng phiên nếu hết giờ → FINISHED, lưu DB
    public boolean closeAuctionIfExpired(int sessionId) { ... }
}
```

**Logic `createAuction`:**
```java
public boolean createAuction(AuctionSession session) {
    // Nếu item chưa có ID → cấp ID mới → lưu item trước
    if (session.getItem() != null && session.getItem().getId() <= 0) {
        session.getItem().setId(itemDAO.allocateNextItemId());
        itemDAO.saveItem(session.getItem());
    }
    // Nếu session chưa có ID → cấp ID mới
    if (session.getId() <= 0) {
        session.setId(auctionSessionDAO.allocateNextSessionId());
    }
    // Lưu session vào DB
    auctionSessionDAO.saveSession(session);
    return true;
}
```

---

## 🧪 Tests liên quan đến Hải

File: `server/src/test/java/com/auction/server/service/`

- `BidServiceTest.java` — 8 test cases cho bid logic
- `AntiSnipingTest.java` — 4 test cases cho anti-sniping
- `ExceptionHandlingTest.java` — 8 test cases cho custom exceptions

**Chạy test:**
```bash
cd server
mvn test
```

---

## ❓ FAQ cho Hải

**Q: Tại sao `AuctionSession` không dùng annotation `@Status` hay gì đó?**  
A: Dùng `AuctionStatus` enum thủ công để kiểm soát chặt chẽ transition (OPEN→RUNNING→FINISHED).

**Q: `isActive()` kiểm tra cả thời gian lẫn status — tại sao không chỉ cần một?**  
A: Tránh race condition: một phiên có thể đã đóng theo thời gian nhưng status chưa cập nhật (AuctionScheduler chạy mỗi 10s). Kiểm tra cả hai đảm bảo an toàn.

**Q: Tại sao `winner` vẫn set trong `updateCurrentPrice()` mà không đợi phiên kết thúc?**  
A: Luôn set winner = người bid cao nhất HIỆN TẠI. Khi phiên kết thúc, `winner` đã đúng rồi, không cần tính lại.

**Q: `Entity` có field `id` — tại sao dùng `int` không dùng `Long`?**  
A: Schema DB dùng `INT NOT NULL`, dùng `int` để khớp, tránh boxing/unboxing không cần thiết.
