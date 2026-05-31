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

> **📌 Ghi chú nộp bài**  
> Bản nộp chính đã hoàn thành và push trước deadline. Các commit sau deadline là hotfix để đảm bảo ổn định khi demo (sửa lỗi wallet transaction bị lặp, dashboard countdown sau anti-sniping). Tất cả tính năng chính đã hoàn thiện trước thời hạn nộp.

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
│              MySQL Database (JDBC)                       │
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

## 🚀 Hướng dẫn cài đặt, build fat JAR và chạy

> Yêu cầu nộp bài: nhánh nộp cuối cùng là `main`, có README rõ ràng và có file executable fat JAR / uber JAR chạy được bằng `java -jar <ten-file>.jar`.

### 1. Yêu cầu môi trường

- **Java 21** — khuyến nghị Eclipse Temurin / Adoptium JDK 21.
- **Maven 3.9+** — dùng để build fat JAR từ source.
- **MySQL 8.0+** — tạo database `auction_system` trước khi chạy server.
- **Git** — để clone/pull source mới nhất.

Kiểm tra môi trường:

```bash
java -version
mvn -version
git --version
```

### 2. Clone hoặc cập nhật source

Clone lần đầu:

```bash
git clone https://github.com/duydo11/auction-system-cua-hai-giang-duy-va-beo.git
cd auction-system-cua-hai-giang-duy-va-beo
```

Nếu đã có project sẵn:

```bash
git pull origin main
```

### 3. Build fat JAR / uber JAR

Cách khuyến nghị trên Windows:

```bat
build-fat-jar.bat
```

Hoặc dùng Maven trực tiếp tại thư mục gốc project:

```bash
mvn clean package -DskipTests
```

Project đang dùng `maven-assembly-plugin` để đóng gói dependencies vào JAR.
Sau khi build thành công, các file JAR nằm tại:

| Module | File JAR cần chạy |
|--------|-------------------|
| Server | `server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar` |
| Client | `client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar` |

### 4. Chạy chương trình bằng JAR

> Luôn chạy **Server trước**, sau đó mới chạy một hoặc nhiều Client.

#### Bước 1 — Chạy Server

Mở terminal/cmd thứ nhất tại thư mục gốc project:

```bash
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Server mặc định lắng nghe:

```text
127.0.0.1:5000
```

Giữ cửa sổ server mở trong suốt quá trình demo/test.

#### Bước 2 — Chạy Client

Mở terminal/cmd thứ hai tại thư mục gốc project:

```bash
java -jar client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### Bước 3 — Chạy nhiều Client

Để demo realtime update hoặc concurrent bidding, mở thêm terminal/cmd khác và chạy lại:

```bash
java -jar client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Ví dụ demo 3 cửa sổ:

- Client 1: đăng nhập Seller để tạo sản phẩm/phiên đấu giá.
- Client 2: đăng nhập Bidder để đặt giá.
- Client 3: đăng nhập Bidder khác hoặc Admin để xem realtime/admin dashboard.

### 5. Thứ tự demo khuyến nghị

1. Chạy `server.jar` trước.
2. Chạy 2-3 cửa sổ `client.jar`.
3. Seller tạo sản phẩm và phiên đấu giá.
4. Bidder 1 đặt giá, Bidder 2 quan sát realtime update.
5. Demo Auto-bid:
   - Mở chi tiết auction bằng tài khoản Bidder.
   - Tick `Auto-bid`.
   - Nhập `Maximum Bid` và `Bid Increment`.
   - Bấm nút bid/start hiện tại.
   - App sẽ đặt bid mở đầu rồi đăng ký auto-bid.
6. Demo Admin dashboard/users/categories nếu cần.

### 6. Database hiện tại

Database của server sử dụng **MySQL qua JDBC**. Trước khi chạy server, cần đảm bảo MySQL đang chạy và database đã được tạo đúng tên.

Thông tin cấu hình mặc định nằm trong `server/src/main/resources/config.properties`:

```properties
db.host=localhost
db.port=3306
db.name=auction_system
db.user=root
db.password=
```

Các lưu ý chính:

- Tạo database `auction_system` trước khi chạy server.
- Cập nhật user/password trong file cấu hình nếu MySQL local không dùng `root` hoặc có mật khẩu.
- Các màn list đã được tối ưu để giảm query N+1.
- Nếu database hoặc server chưa chạy, client sẽ không tải được dashboard/auction list.

Các bảng chính:

```sql
users, bidders, sellers, admins
items, electronics, arts, vehicles
auction_sessions, bids, transactions
```

### 7. Chạy test/smoke test khi cần

Chạy unit tests:

```bash
mvn test
```

Chạy smoke test protocol/server trên Windows PowerShell:

```powershell
cd server
mvn dependency:build-classpath -Dmdep.outputFile=target\cp.txt
$cp = Get-Content target\cp.txt
java -cp "target\classes;$cp" com.auction.server.tools.ProtocolSmokeTest
```

Kết quả mong muốn:

```text
SMOKE_TEST_PASS activeAuctions=... allAuctions=... users=...
```

### 8. Lỗi thường gặp

| Lỗi | Cách xử lý |
|-----|------------|
| `Connection refused 127.0.0.1:5000` | Chưa chạy server hoặc server crash. Chạy server trước client. |
| `Socket closed` | Tắt hết client/server cũ, pull source mới, build lại JAR rồi chạy lại. |
| Load dashboard chậm | Kiểm tra server và MySQL local; lần đầu tải dữ liệu có thể chậm. |
| Không thấy auction mới | Kiểm tra server còn chạy, rồi mở lại dashboard/client. |
| JavaFX không chạy | Kiểm tra đang dùng JDK 21 và đã build đúng client fat JAR. |

### 9. Link báo cáo PDF và video demo

- Thư mục nộp báo cáo PDF và video demo: [Google Drive](https://drive.google.com/drive/folders/1ads3lh6ZC7npi7vPYbzs_Hdh2kKdThKO?usp=sharing).
- Nội dung trong thư mục gồm báo cáo tổng hợp và video demo dự án.

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
