# 📋 Phân Công Giai Đoạn 2 — Sprint Cuối (Tuần 11–15)

> **Mục tiêu**: Từ ~6.7đ hiện tại → **11/10 điểm** (full bắt buộc + full nâng cao)
> **Thời gian**: ~10 ngày làm việc, chia 4 phase

---

## 🔢 Tổng quan điểm cần bù

| Hạng mục | Thiếu | Điểm cần bù |
|----------|-------|-------------|
| Design Pattern (Factory Method) | Chưa có ItemFactory | +0.5 |
| Quản lý sản phẩm (CRUD đầy đủ) | Thiếu sửa/xóa item, Admin dashboard | +0.5 |
| Chức năng đấu giá (auto-close, trạng thái) | Thiếu AuctionStatus, scheduler | +0.3 |
| Xử lý lỗi & ngoại lệ | Thiếu custom exception | +0.4 |
| Unit Test | AuctionTest rỗng, thiếu test logic | +0.2 |
| Code quality | Code duplicate, package thừa | +0.2 |
| Auto-Bidding | Chưa có | +0.5 |
| Anti-sniping | Chưa có | +0.5 |
| Bid History Line Chart | Chưa có | +0.5 |

---

## 👥 Phân công theo người

### 🧠 Hải — Backend Logic

> **Khối lượng: ██████████ 10 task**

#### Phase 1 (ngày 1–3): Fix bắt buộc
- [ ] **[P1-H1]** Tạo `AuctionStatus` enum (`OPEN, RUNNING, FINISHED, PAID, CANCELED`)
  - File: `shared/model/auction/AuctionStatus.java`
  - Thêm field `status` vào `AuctionSession`, default = `OPEN`
  - Cập nhật `isActive()` dùng status thay vì chỉ check time
- [ ] **[P1-H2]** Tạo `ItemFactory` (Factory Method pattern)
  - File: `shared/model/item/ItemFactory.java`
  - Switch theo type → trả về `Electronics` / `Art` / `Vehicle`
  - Cập nhật mọi chỗ `new Electronics(...)` → dùng factory
- [ ] **[P1-H3]** Tạo bộ custom exception
  - `shared/exception/AuctionException.java` (base)
  - `BidTooLowException`, `AuctionClosedException`, `UnauthorizedRoleException`, `InvalidDataException`
  - Thay `println("Invalid bid")` → throw exception cụ thể
- [ ] **[P1-H4]** Bổ sung `closeAuctionIfExpired()` trong `AuctionService`
  - Kiểm tra `endTime < now` → chuyển status FINISHED + xác định winner

#### Phase 2 (ngày 4–6): Chức năng thiếu
- [ ] **[P2-H5]** Tạo `AuctionScheduler` — auto-close phiên
  - Dùng `ScheduledExecutorService`, chạy mỗi 10s
  - Scan DB → đóng phiên hết hạn → broadcast push `AUCTION_CLOSED_PUSH`
- [ ] **[P2-H6]** Logic sửa/xóa sản phẩm (`AuctionService` + `ItemDAO`)
  - Chỉ cho sửa/xóa khi phiên chưa RUNNING
  - Validate quyền seller

#### Phase 3 (ngày 7–9): Nâng cao
- [ ] **[P3-H7]** Auto-Bidding logic
  - File: `server/service/AutoBidService.java`
  - Model: `shared/model/auction/AutoBidConfig.java` (bidderId, sessionId, maxBid, increment)
  - Khi có bid mới → check tất cả auto-bid configs → tự động đặt bid
  - Priority theo thời gian đăng ký, không vượt maxBid
- [ ] **[P3-H8]** Anti-sniping logic
  - Trong `BidService.placeBid()`: nếu bid trong 30s cuối → gia hạn +60s
  - Config: `SNIPE_WINDOW_SEC = 30`, `EXTENSION_SEC = 60`
  - Broadcast push thông báo gia hạn

#### Phase 4 (ngày 10): Polish
- [ ] **[P4-H9]** Review + fix edge cases toàn bộ business logic
- [ ] **[P4-H10]** Viết doc giải thích rule nghiệp vụ cho bảo vệ

**Bàn giao cho Hoàng**: contract mới (MessageType + payload mẫu) cho mỗi feature
**Bàn giao cho Duy**: danh sách case cần test cho mỗi feature

---

### 🎨 Giang — Frontend JavaFX

> **Khối lượng: ██████████ 10 task**

#### Phase 1 (ngày 1–3): Fix bắt buộc
- [ ] **[P1-G1]** Refactor `SceneNavigator` utility
  - Gom tất cả `FXMLLoader.load() + stage.setScene()` duplicate → 1 hàm duy nhất
  - File: `client/util/SceneNavigator.java`
- [ ] **[P1-G2]** BidderDashboard hiển thị data thật
  - Thay `testLoadCards()` mock → gọi `protocol.getActiveAuctions()` thật
  - Mỗi ProductCard bind data từ `AuctionSession`
- [ ] **[P1-G3]** Hiển thị lỗi thân thiện trên UI
  - Thay console `System.err` → Label/Alert trên giao diện
  - Dùng error message từ custom exception (Hải cung cấp)

#### Phase 2 (ngày 4–6): Chức năng thiếu
- [ ] **[P2-G4]** Seller: màn quản lý sản phẩm
  - List sản phẩm đã đăng (TableView)
  - Nút sửa/xóa → gọi protocol
  - Dropdown chọn loại item (Electronics/Art/Vehicle) → dùng ItemFactory
- [ ] **[P2-G5]** Màn chi tiết phiên đấu giá (Auction Detail)
  - Hiển thị: tên item, giá hiện tại, thời gian còn lại, danh sách bid
  - Nút Place Bid
  - Realtime update qua `SceneRealtime.attachAuctionUpdates()`
- [ ] **[P2-G6]** Admin Dashboard (cơ bản)
  - Xem tất cả phiên, tất cả user
  - Nút ban user

#### Phase 3 (ngày 7–9): Nâng cao
- [ ] **[P3-G7]** Auto-Bid UI
  - Form nhập maxBid + increment trong màn Auction Detail
  - Nút "Bật Auto-Bid" → gọi protocol
  - Hiển thị trạng thái auto-bid đang chạy
- [ ] **[P3-G8]** Bid History Line Chart
  - `javafx.scene.chart.LineChart` trong màn Auction Detail
  - Trục X: timestamp, Trục Y: giá bid
  - Load lịch sử + cập nhật realtime khi có push
- [ ] **[P3-G9]** Hiển thị trạng thái phiên (badge OPEN/RUNNING/FINISHED)
  - Đổi màu badge theo status
  - Countdown timer cho phiên đang chạy

#### Phase 4 (ngày 10): Polish
- [ ] **[P4-G10]** Demo rehearsal — chạy full flow, fix UI bug cuối

**Nhận từ Hoàng**: API protocol cụ thể cho từng màn mới
**Nhận từ Hải**: danh sách thông báo lỗi cần hiển thị

---

### 🔌 Hoàng — Networking

> **Khối lượng: ████████░░ 8 task**

#### Phase 1 (ngày 1–3): Fix bắt buộc
- [ ] **[P1-N1]** Thêm MessageType mới
  ```
  UPDATE_ITEM_REQUEST/RESPONSE
  DELETE_ITEM_REQUEST/RESPONSE
  CLOSE_AUCTION_PUSH
  AUCTION_EXTENDED_PUSH (anti-snipe)
  REGISTER_AUTO_BID_REQUEST/RESPONSE
  CANCEL_AUTO_BID_REQUEST/RESPONSE
  BAN_USER_REQUEST/RESPONSE
  GET_ALL_USERS_REQUEST/RESPONSE
  ```
- [ ] **[P1-N2]** Cập nhật `ServerProtocolHandler` — thêm case cho MessageType mới
  - Mỗi case gọi đúng service method (Hải cung cấp)
  - Copy correlationId đúng cách
- [ ] **[P1-N3]** Cập nhật `ClientProtocolHandler` — thêm method mới
  - `updateItem()`, `deleteItem()`, `banUser()`, `getAllUsers()`
  - `registerAutoBid()`, `cancelAutoBid()`

#### Phase 2 (ngày 4–6): Chức năng thiếu
- [ ] **[P2-N4]** Xử lý push mới trong `SocketClient.dispatchIncoming()`
  - `CLOSE_AUCTION_PUSH` → thông báo phiên đã đóng
  - `AUCTION_EXTENDED_PUSH` → thông báo gia hạn anti-snipe
- [ ] **[P2-N5]** Cải thiện error propagation
  - Server trả `errorMessage` cụ thể từ custom exception
  - Client hiển thị đúng message (không mất thông tin)

#### Phase 3 (ngày 7–9): Nâng cao + harden
- [ ] **[P3-N6]** Protocol cho Auto-Bid + Anti-snipe
  - Đảm bảo payload serialize/deserialize đúng `AutoBidConfig`
  - Test gửi/nhận qua socket thực
- [ ] **[P3-N7]** Harden network: log gọn, retry chuẩn
  - Thay `System.out/err` → SLF4J hoặc `java.util.logging`
  - Giảm log spam khi chạy bình thường

#### Phase 4 (ngày 10): Polish
- [ ] **[P4-N8]** Viết doc `docs/protocol.md` — liệt kê tất cả MessageType + payload mẫu

**Nhận từ Hải**: contract nghiệp vụ (input/output mỗi API)
**Bàn giao cho Giang**: hàm protocol cụ thể cho từng thao tác UI
**Bàn giao cho Duy**: danh sách case network cần test

---

### 🛡️ Duy — Test, Quality, Concurrency & Tích hợp

> **Khối lượng: ██████████ 10 task**

#### Phase 1 (ngày 1–3): Unit Test + Cleanup
- [ ] **[P1-D1]** Viết `AuctionSessionTest` (đang rỗng!)
  - `testUpdateCurrentPrice_validBid` — bid hợp lệ → giá cập nhật
  - `testUpdateCurrentPrice_bidTooLow` — bid thấp → giá không đổi
  - `testUpdateCurrentPrice_afterClose` — phiên đóng → reject
  - `testIsActive` — kiểm tra logic thời gian
- [ ] **[P1-D2]** Viết `BidServiceTest`
  - `testPlaceBid_success`
  - `testPlaceBid_invalidSession` → return false
  - `testPlaceBid_concurrent` — 10 thread bid cùng lúc → chỉ 1 thắng mỗi vòng, không lost update
- [ ] **[P1-D3]** Cleanup code toàn repo
  - Xóa package `shared.network` (Request/Response không ai dùng)
  - Xóa `shared.model.Auction`, `shared.model.Bid`, `shared.model.Category` (duplicate/unused)
  - Xóa MockData nếu không còn dùng
  - Thống nhất comment tiếng Việt hoặc tiếng Anh

#### Phase 2 (ngày 4–6): Test mở rộng + CI
- [ ] **[P2-D4]** Viết test cho custom exception (từ Hải)
  - Test từng exception throw đúng case
  - Test `ServerProtocolHandler` trả error message cụ thể
- [ ] **[P2-D5]** Viết `UserServiceTest`
  - `testRegisterDuplicate` → return false
  - `testLoginWrongPassword` → return null
  - `testRegisterDifferentRoles` → đúng subclass
- [ ] **[P2-D6]** Fix CI — test không phụ thuộc DB cloud
  - Option A: Mock DAO trong unit test (Mockito)
  - Option B: Dùng H2 in-memory DB cho test
  - Đảm bảo `mvn test` xanh trên GitHub Actions

#### Phase 3 (ngày 7–9): Test nâng cao
- [ ] **[P3-D7]** Test Auto-Bidding
  - 2 auto-bid cùng phiên → priority đúng thời gian
  - Auto-bid không vượt maxBid
  - Auto-bid + manual bid xen kẽ
- [ ] **[P3-D8]** Test Anti-sniping
  - Bid trong 30s cuối → endTime gia hạn +60s
  - Bid ngoài 30s cuối → endTime không đổi
  - Gia hạn liên tục khi bid liên tục trong vùng snipe
- [ ] **[P3-D9]** Stress test concurrency
  - 50 thread bid đồng thời trên 5 phiên
  - Assert: không lost update, không duplicate winner, giá luôn tăng

#### Phase 4 (ngày 10): Polish
- [ ] **[P4-D10]** Chạy toàn bộ test suite, fix flaky test, viết báo cáo test ngắn
  - Tổng số test case
  - Coverage ước lượng
  - Kết quả CI screenshot

---

## 📅 Timeline Tổng

```
Ngày 1-3 ║ PHASE 1: Fix bắt buộc
─────────╫──────────────────────────────────────────
  Hải    ║ AuctionStatus + ItemFactory + Exceptions + closeAuction
  Giang  ║ SceneNavigator + Data thật Dashboard + Error UI
  Hoàng  ║ MessageType mới + ServerProtocol + ClientProtocol
  Duy    ║ AuctionSessionTest + BidServiceTest + Cleanup code
─────────╫──────────────────────────────────────────
Ngày 4-6 ║ PHASE 2: Chức năng thiếu
─────────╫──────────────────────────────────────────
  Hải    ║ AuctionScheduler + CRUD item logic
  Giang  ║ Seller quản lý SP + Auction Detail + Admin Dashboard
  Hoàng  ║ Push mới + Error propagation
  Duy    ║ Exception test + UserServiceTest + Fix CI
─────────╫──────────────────────────────────────────
Ngày 7-9 ║ PHASE 3: Nâng cao (+1.5đ bonus)
─────────╫──────────────────────────────────────────
  Hải    ║ Auto-Bidding logic + Anti-sniping logic
  Giang  ║ Auto-Bid UI + Line Chart + Status badge
  Hoàng  ║ Protocol Auto-Bid/Snipe + Harden network
  Duy    ║ Test Auto-Bid + Test Anti-snipe + Stress test
─────────╫──────────────────────────────────────────
Ngày 10  ║ PHASE 4: Polish & Demo
─────────╫──────────────────────────────────────────
  Hải    ║ Review edge cases + Viết doc rule
  Giang  ║ Demo rehearsal + Fix UI cuối
  Hoàng  ║ Viết docs/protocol.md
  Duy    ║ Chạy full test + Báo cáo + CI screenshot
```

---

## 🔄 Quy tắc phối hợp giai đoạn này

### Thứ tự merge mỗi phase
```
1. Hải merge trước (model + logic)     ← nền tảng
2. Hoàng merge sau (protocol + network) ← cầu nối
3. Giang merge tiếp (UI)               ← giao diện
4. Duy merge cuối (test + quality)      ← kiểm tra
```

### Checkpoint hàng ngày
Mỗi người cập nhật checkbox `[x]` trong file này khi hoàn thành task.

### Quy tắc branch
- `hai/phase1`, `hai/phase2`, `hai/phase3`
- `giang/phase1`, ...
- `hoang/phase1`, ...
- `duy/phase1`, ...

---

## 📊 Tracking Tiến Độ

### Phase 1 Progress
| Người | Done | Total | % |
|-------|------|-------|---|
| Hải   | 0    | 4     | 0% |
| Giang | 0    | 3     | 0% |
| Hoàng | 0    | 3     | 0% |
| Duy   | 0    | 3     | 0% |

### Phase 2 Progress
| Người | Done | Total | % |
|-------|------|-------|---|
| Hải   | 0    | 2     | 0% |
| Giang | 0    | 3     | 0% |
| Hoàng | 0    | 2     | 0% |
| Duy   | 0    | 3     | 0% |

### Phase 3 Progress
| Người | Done | Total | % |
|-------|------|-------|---|
| Hải   | 0    | 2     | 0% |
| Giang | 0    | 3     | 0% |
| Hoàng | 0    | 2     | 0% |
| Duy   | 0    | 3     | 0% |

### Phase 4 Progress
| Người | Done | Total | % |
|-------|------|-------|---|
| Hải   | 0    | 2     | 0% |
| Giang | 0    | 1     | 0% |
| Hoàng | 0    | 1     | 0% |
| Duy   | 0    | 1     | 0% |

### Tổng: 0/38 tasks (0%)

---

> **Cập nhật lần cuối**: 2026-05-05
> **Người tạo**: Duy (QA Lead)
