# 🗺️ Bản đồ toàn hệ thống — Đọc TRƯỚC khi đọc guide riêng

> File này giải thích toàn bộ hệ thống từ A đến Z để tất cả 3 người (Hải, Hoàng, Duy) nắm được bức tranh chung trước khi đi sâu vào phần của mình.

---

## 📁 Cấu trúc project

```
auction_system/          ← Root Maven project
├── shared/              ← Module dùng chung (client + server đều dùng)
│   └── model, protocol, exception
├── server/              ← Backend (Hải + Hoàng + Duy)
│   └── network, service, dao, config
├── client/              ← Frontend JavaFX (Giang + Hoàng networking)
│   └── controller, network, util
├── pom.xml              ← Root POM (quản lý 3 module)
├── database/            ← SQL scripts cho MySQL
└── .github/workflows/   ← CI/CD (Duy)
```

---

## 🚦 Cách chạy hệ thống

### Chạy Server:
```bash
# Cần MySQL đang chạy, config trong server.properties
mvn clean compile
cd server
mvn exec:java -Dexec.mainClass="com.auction.server.ServerMain"
```

### Chạy Client:
```bash
cd client
mvn javafx:run
```

### ServerMain làm gì khi khởi động:
```java
public class ServerMain {
    public static void main(String[] args) {
        // 1. Khởi động AuctionScheduler (tự động đóng phiên hết giờ, mỗi 10s)
        AuctionScheduler.getInstance().start();
        
        // 2. Khởi động SocketServer lắng nghe TCP
        new SocketServer().start();
        
        // SocketServer sẽ block ở đây, chờ client kết nối
    }
}
```

---

## 🔗 Luồng giao tiếp đầy đủ

### Bước 1: Client khởi động
```
MainApp.start()
  → ClientConnection.getInstance() (Singleton)
  → Đọc client.properties → host="localhost", port=5000
  → Chưa connect (lazy connect)
```

### Bước 2: User đăng nhập
```
LoginController.handleLogin()
  → new ClientProtocolHandler()
  → protocol.login("username", "password")
  → ClientConnection.connect() (lần đầu connect → tạo SocketClient)
  → SocketClient gửi: Message{type=LOGIN_REQUEST, data=["username","password"], correlationId="uuid-123"}
  
SERVER SIDE:
  ClientHandler.run() đọc message
  → ServerProtocolHandler.handleMessage()
  → handleLoginRequest() → UserService.loginUser() → UserDAO.login() → MySQL
  → Nếu OK: trả Message{type=LOGIN_RESPONSE, data=User object, correlationId="uuid-123"}
  
CLIENT SIDE:
  SocketClient.readLoop() nhận response
  → dispatchIncoming(): tìm CompletableFuture theo correlationId="uuid-123"
  → future.complete(response)
  → protocol.login() trả về User object
  → SessionContext.setCurrentUser(user)
  → Navigate tới Dashboard
```

### Bước 3: Bidder đặt giá
```
AuctionDetailsController.handlePlaceBid()
  → protocol.placeBid(sessionId=1, bidderId=5, amount=500000)
  → Gửi: Message{type=PLACE_BID_REQUEST, data=["1","5","500000.0"]}

SERVER:
  ServerProtocolHandler.handlePlaceBid()
  → BidService.placeBid(1, 5, 500000)
  → synchronized(lockForSession(1)) ← Chỉ 1 thread vào cùng lúc cho session #1
  → AuctionSessionDAO.getSessionById(1) ← Load từ DB
  → Tạo Bid object
  → session.updateCurrentPrice(bid) ← Validate + update price
  → BidDAO.saveBid(bid) ← Lưu DB
  → checkAndExtendForAntiSnipe(session) ← Kiểm tra 30s cuối
  → AuctionSessionDAO.updateSession(session) ← Cập nhật DB
  → ClientBroadcastHub.broadcast(AUCTION_UPDATED_PUSH, session)
     ← Gửi cho TẤT CẢ client đang kết nối
  → Trả PLACE_BID_RESPONSE về cho bidder gốc

TẤT CẢ CLIENT:
  SocketClient.readLoop() nhận AUCTION_UPDATED_PUSH (không có correlationId)
  → dispatchIncoming() → RealtimeAuctionBus.dispatch(message)
  → Tất cả listeners được gọi (ProductCardController, BidderDashboardController...)
  → Platform.runLater() → Update UI với giá mới
```

---

## 📨 Bảng tất cả MessageType

| MessageType | Hướng | Mô tả |
|-------------|-------|-------|
| LOGIN_REQUEST | Client→Server | Đăng nhập |
| LOGIN_RESPONSE | Server→Client | Kết quả đăng nhập |
| REGISTER_REQUEST | Client→Server | Đăng ký |
| REGISTER_RESPONSE | Server→Client | Kết quả đăng ký |
| LOGOUT_REQUEST | Client→Server | Đăng xuất |
| VIEW_AUCTIONS_REQUEST | Client→Server | Lấy danh sách phiên |
| VIEW_AUCTIONS_RESPONSE | Server→Client | Danh sách phiên |
| CREATE_AUCTION_REQUEST | Client→Server | Seller tạo phiên |
| CREATE_AUCTION_RESPONSE | Server→Client | Kết quả tạo phiên |
| PLACE_BID_REQUEST | Client→Server | Bidder đặt giá |
| PLACE_BID_RESPONSE | Server→Client | Kết quả đặt giá |
| GET_BIDS_REQUEST | Client→Server | Lấy lịch sử bid |
| GET_BIDS_RESPONSE | Server→Client | Lịch sử bid |
| UPDATE_ITEM_REQUEST | Client→Server | Sửa sản phẩm |
| DELETE_ITEM_REQUEST | Client→Server | Xóa sản phẩm |
| REGISTER_AUTO_BID_REQUEST | Client→Server | Đăng ký auto-bid |
| CANCEL_AUTO_BID_REQUEST | Client→Server | Hủy auto-bid |
| BAN_USER_REQUEST | Client→Server | Admin ban user |
| GET_ALL_USERS_REQUEST | Client→Server | Admin lấy danh sách user |
| **AUCTION_UPDATED_PUSH** | **Server→AllClients** | **Giá vừa thay đổi** |
| **AUCTION_CREATED_PUSH** | **Server→AllClients** | **Phiên mới được tạo** |
| **CLOSE_AUCTION_PUSH** | **Server→AllClients** | **Phiên vừa đóng** |
| **AUCTION_EXTENDED_PUSH** | **Server→AllClients** | **Anti-snipe: gia hạn** |

> **PUSH types** = Server chủ động gửi, không có correlationId, client nhận qua RealtimeAuctionBus

---

## 🗄️ Database Schema

```sql
-- Bảng users (cha)
users: id, username, password, email

-- Bảng role-specific (con)
bidders:  user_id → FK users.id, account_balance
sellers:  user_id → FK users.id, rating
admins:   user_id → FK users.id, access_level

-- Sản phẩm
items:       id, name, description, seller_id → FK users.id
electronics: item_id → FK items.id, warranty_months
arts:        item_id → FK items.id, author
vehicles:    item_id → FK items.id, brand

-- Phiên đấu giá
auction_sessions: id, item_id, seller_id, winner_id, starting_price, current_price, start_time, end_time

-- Lịch sử bid
bids: id, bidder_id, auction_session_id, amount, time
```

---

## 👥 Ai làm gì — Bảng phân công chi tiết

| File / Class | Người phụ trách | Nhiệm vụ |
|---|---|---|
| `shared/model/*` | **Hải** | Thiết kế class hierarchy |
| `shared/exception/*` | **Hải** | Custom exceptions |
| `shared/protocol/Message.java` | **Hoàng** | Gói tin giao tiếp |
| `shared/protocol/MessageType.java` | **Hoàng** | 40+ loại message |
| `server/network/SocketServer.java` | **Hoàng** | TCP listener |
| `server/network/ClientHandler.java` | **Hoàng** | Xử lý từng client |
| `server/network/ClientBroadcastHub.java` | **Hoàng** | Broadcast push |
| `server/network/ServerProtocolHandler.java` | **Hoàng** | Router cho message |
| `server/service/AuctionService.java` | **Hải** | CRUD phiên, đóng phiên |
| `server/service/BidService.java` | **Duy** | Đặt giá + concurrency lock |
| `server/service/UserService.java` | **Hải/Duy** | CRUD user |
| `server/service/AutoBidService.java` | **Duy** | Auto-bid với PriorityQueue |
| `server/service/AuctionScheduler.java` | **Duy** | Tự động đóng phiên |
| `server/dao/*` | **Hải/Duy** | Kết nối MySQL |
| `client/network/SocketClient.java` | **Hoàng** | TCP client, read loop |
| `client/network/ClientConnection.java` | **Hoàng** | Singleton kết nối |
| `client/network/ClientProtocolHandler.java` | **Hoàng** | API cho UI |
| `client/RealtimeAuctionBus.java` | **Hoàng** | Observer realtime |
| `client/controller/*` | **Giang** | JavaFX controllers |
| `client/util/SceneNavigator.java` | **Giang** | Điều hướng màn hình |
| `server/src/test/*` | **Duy** | 41 unit tests |
| `.github/workflows/*` | **Duy** | CI/CD |

---

## 🔍 Cách đọc code khi bị hỏi "cái này làm gì?"

### Tip 1: Trace theo MessageType
Nếu thấy `MessageType.PLACE_BID_REQUEST`:
1. Client gửi ở: `ClientProtocolHandler.placeBid()`
2. Server nhận ở: `ServerProtocolHandler.handlePlaceBid()`
3. Logic thực tế ở: `BidService.placeBid()`

### Tip 2: Trace theo flow khi có lỗi
```
Lỗi "Bid too low" ở client:
→ ClientProtocolHandler.placeBid() returns false
→ response.isSuccess() == false
→ response.getData() = "Bid too low or session closed"
→ Server trả ở: handlePlaceBid() dòng 163
→ Vì BidService.placeBid() trả false
→ Vì session.getBids().size() không tăng
→ Vì AuctionSession.updateCurrentPrice() không add bid
→ Vì bid.getAmount() <= currentPrice
```

### Tip 3: Khi thấy annotation
- `@FXML` → Giang dùng, liên kết với FXML file
- `@Getter @Setter` → Lombok tự generate getter/setter
- `@Data` → Lombok tự generate getter/setter/equals/hashCode/toString
- `@NoArgsConstructor` → Constructor không tham số
- `@AllArgsConstructor` → Constructor tất cả tham số

---

## 📄 Files guide chi tiết cho từng người

- **Hải:** [CODEBASE_GUIDE_HAI.md](CODEBASE_GUIDE_HAI.md) — Domain Model, Business Logic
- **Hoàng:** [CODEBASE_GUIDE_HOANG.md](CODEBASE_GUIDE_HOANG.md) — Networking, Socket, Protocol
- **Duy:** [CODEBASE_GUIDE_DUY.md](CODEBASE_GUIDE_DUY.md) — Concurrency, Testing, CI/CD
