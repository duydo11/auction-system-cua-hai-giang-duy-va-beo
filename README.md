# 🏛️ Online Auction System

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/Maven-3.9-red?style=for-the-badge&logo=apachemaven)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![CI](https://img.shields.io/badge/CI-GitHub_Actions-black?style=for-the-badge&logo=githubactions)

**Hệ thống đấu giá trực tuyến theo kiến trúc Client-Server**  
*Bài tập lớn — Lập trình nâng cao*

</div>

---

## 📖 Giới thiệu

Hệ thống đấu giá trực tuyến cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua một sản phẩm trong khoảng thời gian xác định. Hệ thống hỗ trợ ba vai trò: **Bidder** (người mua), **Seller** (người bán), **Admin** (quản trị), giao tiếp qua **TCP Socket** với giao thức Message tùy chỉnh.

---

## ✨ Tính năng

### Bắt buộc
- 🔐 **Đăng ký / Đăng nhập** — 3 vai trò (Bidder, Seller, Admin)
- 📦 **Quản lý sản phẩm** — Thêm / sửa / xóa (Electronics, Art, Vehicle)
- 🔨 **Đặt giá** — Validate bid, cập nhật giá realtime
- ⏰ **Tự động đóng phiên** — Scheduler tự kết thúc khi hết thời gian
- ⚠️ **Xử lý ngoại lệ** — Custom exceptions rõ ràng
- 🖥️ **Giao diện JavaFX** — 3 dashboard theo vai trò

### Nâng cao
- 🤖 **Auto-Bidding** — Đấu giá tự động với `maxBid` và `increment`
- 🔒 **Concurrent Bidding** — Lock per-session, 50-thread stress test
- 🛡️ **Anti-sniping** — Tự động gia hạn +60s nếu có bid trong 30s cuối
- 📡 **Realtime Push** — Observer pattern, broadcast tới tất cả client

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────┐
│                      CLIENT (JavaFX)                     │
│  Login → Dashboard → Bid → View Auctions                │
│  MVC: FXML (View) + Controller + ClientProtocolHandler   │
└───────────────────────┬─────────────────────────────────┘
                        │ TCP Socket (Message Protocol)
┌───────────────────────▼─────────────────────────────────┐
│                      SERVER                              │
│  ServerProtocolHandler → Service → DAO → MySQL           │
│  ClientBroadcastHub (push realtime)                      │
└───────────────────────┬─────────────────────────────────┘
                        │ JDBC
┌───────────────────────▼─────────────────────────────────┐
│           MySQL Database (Aiven Cloud)                   │
│  users / items / auction_sessions / bids                 │
└─────────────────────────────────────────────────────────┘
```

### Module structure
```
auction-system/
├── shared/          # Dùng chung: Model, MessageType, Exception
│   ├── model/       # Entity, User, Item, AuctionSession, Bid
│   ├── protocol/    # Message, MessageType (14 types)
│   └── exception/   # Custom exceptions (5 loại)
├── server/          # Backend
│   ├── service/     # BidService, AuctionService, AutoBidService...
│   ├── dao/         # DAO layer → MySQL
│   └── network/     # ServerProtocolHandler, ClientBroadcastHub
└── client/          # Frontend JavaFX
    ├── controller/  # BidderScene, SellerScene, AdminScene
    └── network/     # ClientProtocolHandler, SocketClient
```

---

## ⚙️ Design Patterns

| Pattern | Vị trí | Mô tả |
|---------|--------|-------|
| **Singleton** | `ServiceRegistry`, `ClientConnection` | Quản lý kết nối |
| **Factory Method** | `ItemFactory` | Tạo Electronics / Art / Vehicle |
| **Observer** | `RealtimeAuctionBus`, `ClientBroadcastHub` | Realtime price update |
| **Strategy** | `AutoBidService` (PriorityQueue) | Xử lý auto-bid theo priority |

---

## 👥 Thành viên & Phân công

| Thành viên | Vai trò | Nhiệm vụ chính | Chi tiết |
|------------|---------|----------------|---------|
| **Hải** | Backend Logic | Domain Model & Business Rules | Thiết kế `User`, `Item`, `AuctionSession`, `Bid`; viết rule nghiệp vụ `checkValidBid`, `updateWinner`, `closeAuctionIfExpired`; custom exceptions |
| **Duy** | QA & Concurrency | Testing + CI/CD + Concurrency | Viết 41 unit tests (JUnit 5 + Mockito); lock per-session chống race condition; stress test 50 threads; CI/CD GitHub Actions (3 OS) |
| **Hoàng** | Networking | Socket Protocol | Xây dựng TCP socket client/server; chuẩn hóa 14 MessageTypes; `ClientProtocolHandler` (14 methods); `ClientBroadcastHub` (realtime push) |
| **Giang** | Frontend UI | JavaFX Interface | Thiết kế 42 controller + 50+ FXML; 3 dashboard (Bidder, Seller, Admin); card components; scene navigation |

### 📊 Chi tiết nhiệm vụ

<details>
<summary><b>Hải — Backend Logic (Cái não)</b></summary>

- Thiết kế cây kế thừa: `Entity → User → Bidder/Seller/Admin`, `Entity → Item → Electronics/Art/Vehicle`
- Thiết kế `AuctionSession` với state machine: `OPEN → RUNNING → FINISHED → CANCELED`
- Viết rule đấu giá: `updateCurrentPrice()`, `isActive()`, `closeAuctionIfExpired()`
- Implement `AuctionScheduler` — tự động đóng phiên theo timer
- Implement `AutoBidService` — xử lý auto-bid với PriorityQueue
- Thiết kế 5 custom exceptions: `BidTooLowException`, `AuctionClosedException`, `UnauthorizedRoleException`, `InvalidDataException`, `AuctionException`
- Viết DAO layer: `UserDAO`, `ItemDAO`, `AuctionSessionDAO`, `BidDAO`

</details>

<details>
<summary><b>Duy — QA & Concurrency (Cảnh sát)</b></summary>

- Thiết kế lock per-session (`ConcurrentHashMap<Integer, Object>`) trong `BidService`
- Viết 41 unit tests: `BidServiceTest`, `UserServiceTest`, `AutoBidServiceTest`, `AntiSnipingTest`, `ExceptionHandlingTest`
- Viết `ConcurrencyStressTest` — 50 threads đặt giá đồng thời trên 5 phiên
- Setup H2 in-memory DB cho CI (không cần MySQL thật)
- Cấu hình GitHub Actions: matrix 3 OS (Ubuntu, Windows, macOS), auto compile + test
- Fix Java 21 + Mockito 5.12.0 compatibility
- Viết `module-info.java` cho client + server

</details>

<details>
<summary><b>Hoàng — Networking (Cánh tay)</b></summary>

- Thiết kế TCP socket: `SocketServer` đa luồng, `SocketClient` có retry
- Chuẩn hóa giao thức: `Message` + 14 `MessageType` (auth, auction, bid, admin, push)
- Implement `ClientProtocolHandler` với 14 phương thức: `login`, `register`, `getActiveAuctions`, `placeBid`, `registerAutoBid`, `cancelAutoBid`, `banUser`, `getAllUsers`...
- Implement `ServerProtocolHandler` xử lý 14 loại request
- Implement `ClientBroadcastHub` — push realtime tới tất cả client: `PRICE_UPDATE_PUSH`, `AUCTION_EXTENDED_PUSH`, `BID_PLACED_PUSH`

</details>

<details>
<summary><b>Giang — Frontend UI (Gương mặt)</b></summary>

- Thiết kế 42 JavaFX controller tổ chức theo scene: `BidderScene/`, `SellerScene/`, `AdminScene/`, `ActionsScene/`, `Card/`
- Thiết kế 50+ FXML layout với SceneBuilder
- Implement 3 dashboard chính: `BidderDashboard`, `SellerDashboard`, `AdminDashboard`
- Card components: `ProductCard`, `HistoryCard`, `TransHisCard`
- Action dialogs: `AddProductDialog`, `AuctionDetailsForBidder`, `DepositAction`, `WithdrawAction`
- Scene navigation: `SceneNavigator` utility
- Login/Register với validation

</details>

---

## 🚀 Hướng dẫn chạy

### Yêu cầu
- Java 21 (Eclipse Temurin)
- Maven 3.9+
- MySQL 8.0 (hoặc dùng Aiven cloud DB đã config sẵn)

### Bước 1: Clone & Build
```bash
git clone https://github.com/duydo11/auction-system-cua-hai-giang-duy-va-beo.git
cd auction-system
mvn clean compile
```

### Bước 2: Chạy Server
```bash
cd server
mvn exec:java -Dexec.mainClass="com.auction.server.ServerMain"
```

### Bước 3: Chạy Client
```bash
cd client
mvn javafx:run
```

### Chạy Unit Tests
```bash
mvn test
# 41 tests, 100% pass (không cần MySQL)
```

---

## 🧪 Testing

| Test Suite | Tests | Coverage |
|------------|-------|---------|
| `BidServiceTest` | 8 | Bid validation, price update |
| `UserServiceTest` | 12 | Login, register, roles |
| `AutoBidServiceTest` | 8 | Auto-bid register, cancel, process |
| `AntiSnipingTest` | 4 | 30s window, +60s extension |
| `ExceptionHandlingTest` | 8 | Custom exceptions |
| `ConcurrencyStressTest` | 2 | 50 threads × 500 bids |
| **Total** | **41** | **100% pass** |

CI/CD: GitHub Actions chạy tự động trên **Ubuntu + Windows + macOS** khi push/PR.

---

## 📂 Cấu trúc Database

```sql
users           -- id, username, password, email
├── bidders     -- user_id, account_balance
├── sellers     -- user_id, rating
└── admins      -- user_id, access_level

items           -- id, name, description, seller_id
├── electronics -- item_id, warranty_months
├── arts        -- item_id, author
└── vehicles    -- item_id, brand

auction_sessions -- id, item_id, seller_id, winner_id, starting_price, current_price, start_time, end_time
bids             -- id, bidder_id, auction_session_id, amount, time
```

---

## 📄 Tài liệu

- [`docs/api.md`](docs/api.md) — API Protocol documentation
- [`docs/database.md`](docs/database.md) — Database schema
- [`docs/design.md`](docs/design.md) — Design decisions
- [`docs/diagrams/`](docs/diagrams/) — 12 UML diagrams (PlantUML)

---

## 📜 License

MIT License — Bài tập lớn Lập trình nâng cao
