# 🎓 Barem chấm điểm → Nói gì khi vấn đáp → Code chứng minh ở đâu

> Mục tiêu: bám đúng từng dòng trong barem. Mỗi mục gồm: **nên nói gì**, **mở file nào để chứng minh**, và **ý chính nếu thầy hỏi sâu**. Không chia người trình bày trong file này.

---

## 0. Câu mở đầu chung

**Nên nói:**

> Nhóm em xây dựng hệ thống đấu giá trực tuyến bằng Java 21, JavaFX, Maven và MySQL theo kiến trúc Client-Server. Client JavaFX giao tiếp với Server qua TCP Socket bằng `Message` protocol. Server chia tầng rõ: Network → Service → DAO → Database. Hệ thống có 3 vai trò Bidder, Seller, Admin; hỗ trợ đăng nhập/đăng ký, quản lý sản phẩm, đặt giá, realtime update, auto-bidding, anti-sniping, bid history chart, unit test và CI/CD.

**Mở để chứng minh:**

- `README.md` phần `Giới thiệu`, `Tính năng`, `Kiến trúc hệ thống`, `Design Patterns`.
- `shared/`, `server/`, `client/` để chứng minh kiến trúc 3 module.

---

# I. Thiết kế lớp và cây kế thừa

## 1. Xác định và triển khai các lớp chính — 0.5 điểm

**Nên nói:**

> Các lớp chính được đặt trong module `shared` để client và server dùng chung. Nhóm có lớp nền `Entity`, sau đó triển khai các nhóm model chính: `User`, `Item`, `AuctionSession`, `Bid`, `Transaction`. `User` tách thành `Bidder`, `Seller`, `Admin`; `Item` tách thành `Electronics`, `Art`, `Vehicle`. Đây là các object cốt lõi phản ánh nghiệp vụ đấu giá.

**Code chứng minh:**

- `shared/src/main/java/com/auction/shared/model/Entity.java`
- `shared/src/main/java/com/auction/shared/model/user/User.java`
- `shared/src/main/java/com/auction/shared/model/user/Bidder.java`
- `shared/src/main/java/com/auction/shared/model/user/Seller.java`
- `shared/src/main/java/com/auction/shared/model/user/Admin.java`
- `shared/src/main/java/com/auction/shared/model/item/Item.java`
- `shared/src/main/java/com/auction/shared/model/item/Electronics.java`
- `shared/src/main/java/com/auction/shared/model/item/Art.java`
- `shared/src/main/java/com/auction/shared/model/item/Vehicle.java`
- `shared/src/main/java/com/auction/shared/model/auction/AuctionSession.java`
- `shared/src/main/java/com/auction/shared/model/Bid.java`
- `shared/src/main/java/com/auction/shared/model/user/Transaction.java`

**Nếu thầy hỏi sâu:**

> `AuctionSession` là model trung tâm vì nó liên kết seller, item, winner, giá hiện tại, thời gian, status và danh sách bid.

---

## 2. Áp dụng OOP: Encapsulation, Inheritance, Polymorphism, Abstraction — 1 điểm

**Nên nói:**

> Encapsulation thể hiện qua private fields và getter/setter trong model. Inheritance thể hiện ở cây `User → Bidder/Seller/Admin` và `Item → Electronics/Art/Vehicle`. Abstraction thể hiện ở `User`/`Item` là lớp cha trừu tượng hoặc lớp nền dùng chung, tránh tạo object quá chung chung. Polymorphism thể hiện khi service/DAO xử lý kiểu cha `User` hoặc `Item`, nhưng runtime object có thể là từng subclass cụ thể.

**Code chứng minh:**

- Encapsulation: `User.java`, `Item.java`, `AuctionSession.java`
- Inheritance/Abstraction: `Bidder.java`, `Seller.java`, `Admin.java`, `Electronics.java`, `Art.java`, `Vehicle.java`
- Polymorphism/Factory: `shared/src/main/java/com/auction/shared/model/item/ItemFactory.java`
- Business behavior trong object: `AuctionSession.java` với các method như kiểm tra trạng thái, cập nhật giá/winner.

**Nếu thầy hỏi sâu:**

> Nếu thêm loại sản phẩm mới, nhóm chỉ cần thêm subclass mới của `Item`, cập nhật `ItemFactory` và DAO mapping, không cần sửa toàn bộ luồng đấu giá.

---

## 3. Áp dụng design pattern phù hợp — 1 điểm

**Nên nói:**

> Nhóm có nhiều design pattern phù hợp. `Factory Method` dùng ở `ItemFactory` để tạo đúng loại item. `Singleton`/shared connection dùng cho registry hoặc client connection để tránh tạo nhiều service/socket rời rạc. `Observer` dùng cho realtime update: server broadcast, client listener nhận push và update UI. Auto-bidding dùng `PriorityBlockingQueue` để xử lý các config theo thứ tự ưu tiên.

**Code chứng minh:**

- Factory Method: `shared/src/main/java/com/auction/shared/model/item/ItemFactory.java`
- Singleton/shared registry: `server/src/main/java/com/auction/server/ServiceRegistry.java` nếu có trong source, hoặc `client/src/main/java/com/auction/client/network/ClientConnection.java`
- Observer/realtime: `server/src/main/java/com/auction/server/network/ClientBroadcastHub.java`, `client/src/main/java/com/auction/client/RealtimeAuctionBus.java`
- Priority queue auto-bid: `server/src/main/java/com/auction/server/service/AutoBidService.java`
- README phần `Design Patterns`.

**Nếu thầy hỏi sâu:**

> Observer hợp lý vì nhiều client cần được thông báo khi bid mới, auction đóng hoặc anti-sniping gia hạn mà không cần tự polling liên tục.

---

# II. Chức năng chính

## 4. Quản lý người dùng, sản phẩm — 1 điểm

**Nên nói:**

> Quản lý người dùng gồm đăng ký, đăng nhập, phân role Bidder/Seller/Admin, admin quản lý user và wallet transaction. Quản lý sản phẩm/auction gồm seller tạo item, tạo phiên đấu giá, xem listing, update/delete theo điều kiện hợp lệ. Controller UI không truy cập DB trực tiếp mà gọi API qua `ClientProtocolHandler`, server xử lý qua Service và DAO.

**Code chứng minh:**

- User backend: `server/src/main/java/com/auction/server/service/UserService.java`, `server/src/main/java/com/auction/server/dao/UserDAO.java`
- User UI: `client/src/main/java/com/auction/client/controller/LoginController.java`, `RegisterController.java`, `AdminScene/UserManagementController.java`
- Product/Auction backend: `server/src/main/java/com/auction/server/service/AuctionService.java`, `ItemDAO.java`, `AuctionSessionDAO.java`
- Product/Auction UI: `client/src/main/java/com/auction/client/controller/ActionsScene/AddProductDialogController.java`, `SellerScene/MyListingController.java`
- Protocol: `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`, `server/src/main/java/com/auction/server/network/ServerProtocolHandler.java`

**Nếu thầy hỏi sâu:**

> User và product đều đi qua luồng UI → protocol → socket → server handler → service → DAO → MySQL, nên tách biệt giao diện và database.

---

## 5. Chức năng đấu giá — 1 điểm

**Nên nói:**

> Luồng đặt giá bắt đầu từ màn detail của bidder. Controller lấy amount người dùng nhập, validate cơ bản rồi gọi `ClientProtocolHandler`. Request đi qua socket tới `ServerProtocolHandler`, sau đó `BidService.placeBid()` xử lý nghiệp vụ: lock theo session, reload session/user, validate giá, tạo `Bid`, cập nhật currentPrice/winner, lưu DB, xử lý anti-sniping/auto-bid nếu cần và broadcast realtime cho các client.

**Code chứng minh:**

- UI đặt giá: `client/src/main/java/com/auction/client/controller/ActionsScene/AuctionDetailsforBidderController.java`
- Client API: `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`
- Socket client: `client/src/main/java/com/auction/client/network/SocketClient.java`
- Server route: `server/src/main/java/com/auction/server/network/ServerProtocolHandler.java`
- Logic chính: `server/src/main/java/com/auction/server/service/BidService.java`
- Domain rule: `shared/src/main/java/com/auction/shared/model/auction/AuctionSession.java`
- DB lưu bid: `server/src/main/java/com/auction/server/dao/BidDAO.java`

**Flow nên nói:**

```text
UI nhập bid → ClientProtocolHandler → SocketClient → ServerProtocolHandler
→ BidService.placeBid → BidDAO/AuctionSessionDAO → broadcast realtime → UI refresh
```

---

## 6. Xử lý lỗi & ngoại lệ — 1 điểm

**Nên nói:**

> Nhóm xử lý lỗi ở nhiều lớp. UI validate input rỗng/sai định dạng. Backend validate lại bằng service/domain để tránh client gửi dữ liệu sai. Các lỗi nghiệp vụ như bid thấp, auction đóng, sai role, dữ liệu không hợp lệ được biểu diễn bằng custom exception hoặc response error message. Client nhận error và hiển thị thông báo cho người dùng.

**Code chứng minh:**

- Custom exceptions: `shared/src/main/java/com/auction/shared/exception/`
- Bid validation: `server/src/main/java/com/auction/server/service/BidService.java`
- Domain validation: `shared/src/main/java/com/auction/shared/model/auction/AuctionSession.java`
- Protocol error response: `server/src/main/java/com/auction/server/network/ServerProtocolHandler.java`, `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`
- UI error message: `AuctionDetailsforBidderController.java`, `LoginController.java`, `RegisterController.java`
- Test exception: `server/src/test/java/com/auction/server/service/ExceptionHandlingTest.java`

**Nếu thầy hỏi sâu:**

> Không tin hoàn toàn vào UI validation; backend vẫn là nơi quyết định dữ liệu hợp lệ vì client có thể gửi request sai.

---

# III. Kỹ thuật quan trọng & concurrency

## 7. Xử lý đấu giá đồng thời an toàn — 1 điểm

**Nên nói:**

> Vì nhiều bidder có thể bid cùng lúc, nhóm dùng lock theo từng `sessionId` trong `BidService`. Mỗi phiên có một lock riêng bằng `ConcurrentHashMap<Integer, Object>`. Như vậy các bid trong cùng phiên được xử lý tuần tự để tránh lost update/race condition, nhưng các phiên khác vẫn có thể chạy song song. Nhóm có stress test nhiều thread để kiểm tra.

**Code chứng minh:**

- Lock/concurrency: `server/src/main/java/com/auction/server/service/BidService.java`
- Stress test: `server/src/test/java/com/auction/server/service/ConcurrencyStressTest.java`
- Bid unit test: `server/src/test/java/com/auction/server/service/BidServiceTest.java`

**Nếu thầy hỏi sâu:**

> Lock toàn hệ thống sẽ làm chậm mọi phiên; lock theo session cân bằng giữa an toàn và hiệu năng.

---

## 8. Realtime update Observer/Socket — 0.5 điểm

**Nên nói:**

> Server broadcast push message khi có bid mới, auction mới, auction đóng hoặc anti-sniping gia hạn. `SocketClient` ở client có reader thread đọc liên tục. Nếu message là push, nó không trả về như response thường mà dispatch vào `RealtimeAuctionBus`. Các controller đã đăng ký listener sẽ update card/detail/chart/countdown trên JavaFX thread.

**Code chứng minh:**

- Server broadcast: `server/src/main/java/com/auction/server/network/ClientBroadcastHub.java`
- Server socket/client handler: `SocketServer.java`, `ClientHandler.java`
- Client socket reader: `client/src/main/java/com/auction/client/network/SocketClient.java`
- Event bus: `client/src/main/java/com/auction/client/RealtimeAuctionBus.java`
- UI listener: `BidderDashboardController.java`, `ProductCardController.java`, `AuctionDetailsforBidderController.java`
- Message types: `shared/src/main/java/com/auction/shared/protocol/MessageType.java`

**Nếu thầy hỏi sâu:**

> `Platform.runLater()` cần dùng khi update UI từ push thread vì JavaFX chỉ cho phép sửa UI trên JavaFX Application Thread.

---

# IV. Tích hợp, kiến trúc & chất lượng mã

## 9. Thiết kế kiến trúc Client-Server rõ ràng — 0.5 điểm

**Nên nói:**

> Project tách thành 3 module Maven: `shared`, `server`, `client`. `shared` chứa model, protocol, exception dùng chung. `client` chỉ làm UI và gửi request qua socket. `server` nhận request, xử lý nghiệp vụ và truy cập MySQL. Client không query database trực tiếp, đảm bảo đúng kiến trúc Client-Server.

**Code chứng minh:**

- Root Maven: `pom.xml`
- Modules: `shared/pom.xml`, `server/pom.xml`, `client/pom.xml`
- Client entry: `client/src/main/java/com/auction/client/MainApp.java`
- Server entry: `server/src/main/java/com/auction/server/ServerMain.java`
- README phần `Kiến trúc hệ thống` và `Module structure`.

---

## 10. MVC JavaFX + Controller-Model-DAO server — 0.5 điểm

**Nên nói:**

> Client theo MVC của JavaFX: FXML là View, Controller là nơi bind dữ liệu và xử lý event, Model lấy từ module `shared`. Controller gọi `ClientProtocolHandler`, không gọi DAO. Server cũng chia tầng: `ServerProtocolHandler` nhận request, Service xử lý nghiệp vụ, DAO truy cập database.

**Code chứng minh:**

- View: `client/src/main/resources/fxml/`
- Controller: `client/src/main/java/com/auction/client/controller/`
- Model: `shared/src/main/java/com/auction/shared/model/`
- Client API: `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`
- Server handler: `server/src/main/java/com/auction/server/network/ServerProtocolHandler.java`
- Service: `server/src/main/java/com/auction/server/service/`
- DAO: `server/src/main/java/com/auction/server/dao/`

**Nếu thầy hỏi sâu:**

> FXML liên kết controller bằng `fx:controller`, field bằng `fx:id`, và event bằng `onAction`/`onMouseClicked`.

---

## 11. Maven/Gradle, coding convention tốt, mã nguồn sạch — 0.5 điểm

**Nên nói:**

> Nhóm dùng Maven multi-module để build toàn bộ project. README có hướng dẫn môi trường, build fat JAR bằng Maven Assembly Plugin và script `build-fat-jar.bat`. Code được tách package rõ ràng theo trách nhiệm: model, protocol, service, dao, network, controller, util, test.

**Code chứng minh:**

- `pom.xml`, `shared/pom.xml`, `server/pom.xml`, `client/pom.xml`
- `build-fat-jar.bat`
- README phần `Hướng dẫn cài đặt, build fat JAR và chạy`
- Package structure: `shared/model`, `server/service`, `server/dao`, `server/network`, `client/controller`, `client/network`, `client/util`

---

## 12. Unit Test JUnit cho logic quan trọng — 0.5 điểm

**Nên nói:**

> Nhóm có test cho logic đặt giá, auto-bid, anti-sniping, exception handling, user service và stress test concurrency. Test dùng JUnit 5, Mockito/H2 để chạy độc lập hơn với môi trường thật.

**Code chứng minh:**

- `server/src/test/java/com/auction/server/service/BidServiceTest.java`
- `server/src/test/java/com/auction/server/service/AutoBidServiceTest.java`
- `server/src/test/java/com/auction/server/service/AntiSnipingTest.java`
- `server/src/test/java/com/auction/server/service/ConcurrencyStressTest.java`
- `server/src/test/java/com/auction/server/service/ExceptionHandlingTest.java`
- `server/src/test/java/com/auction/server/service/UserServiceTest.java`
- `server/pom.xml` phần dependencies test.

**Nếu thầy hỏi sâu:**

> Test quan trọng nhất là bid/concurrency vì đây là phần dễ lỗi nhất khi nhiều người đặt giá đồng thời.

---

## 13. CI/CD GitHub Actions — 0.5 điểm

**Nên nói:**

> Nhóm có GitHub Actions để tự động build/test Maven khi push hoặc pull request. Badge CI cũng được đưa lên README để thể hiện trạng thái build. Ngoài workflow Maven, nhóm còn có workflow tạo diagram tài liệu.

**Code chứng minh:**

- `.github/workflows/maven.yml`
- `.github/workflows/generate-diagrams.yml`
- README dòng badge CI ở đầu file.

**Nếu thầy hỏi sâu:**

> CI giúp phát hiện lỗi compile/test sớm trên GitHub, tránh chỉ chạy được trên máy một thành viên.

---

# V. Chức năng nâng cao

## 14. Auto-Bidding: maxBid, increment, PriorityQueue — 0.5 điểm

**Nên nói:**

> Auto-bidding cho phép bidder cấu hình `maxBid` và `increment`. Khi có bid mới, `AutoBidService` kiểm tra các config trong phiên, chọn auto-bid phù hợp bằng hàng đợi ưu tiên và tự đặt bid tiếp theo nhưng không vượt `maxBid`. Tính năng này giúp bidder không cần ngồi canh màn hình liên tục.

**Code chứng minh:**

- Model config: `shared/src/main/java/com/auction/shared/model/auction/AutoBidConfig.java`
- Logic: `server/src/main/java/com/auction/server/service/AutoBidService.java`
- Gọi từ bid flow: `server/src/main/java/com/auction/server/service/BidService.java`
- UI bật auto-bid: `client/src/main/java/com/auction/client/controller/ActionsScene/AuctionDetailsforBidderController.java`
- Test: `server/src/test/java/com/auction/server/service/AutoBidServiceTest.java`

---

## 15. Anti-sniping khi bid cuối — 0.5 điểm

**Nên nói:**

> Anti-sniping ngăn việc người dùng đặt bid sát giờ cuối để thắng không công bằng. Nếu bid được đặt trong 30 giây cuối, server tự gia hạn `endTime` thêm 60 giây, lưu session mới và broadcast `AUCTION_EXTENDED_PUSH`. Client nhận push để cập nhật countdown và cảnh báo UI.

**Code chứng minh:**

- Logic gia hạn: `server/src/main/java/com/auction/server/service/BidService.java`
- Push type: `shared/src/main/java/com/auction/shared/protocol/MessageType.java`
- Broadcast: `server/src/main/java/com/auction/server/network/ClientBroadcastHub.java`
- UI countdown/warning: `AuctionDetailsforBidderController.java`, `AuctionDetailsforSellerController.java`
- Test: `server/src/test/java/com/auction/server/service/AntiSnipingTest.java`

---

## 16. Bid History Visualization: line chart realtime — 0.5 điểm

**Nên nói:**

> Màn detail auction hiển thị lịch sử giá bằng `LineChart`. Controller gọi API lấy `BidHistory`, mỗi `Bid` trở thành một điểm trên chart theo thời gian và amount. Khi có realtime push bid mới, controller reload lại chart để biểu đồ cập nhật gần như realtime.

**Code chứng minh:**

- UI chart bidder: `client/src/main/resources/fxml/ActionsScene/AuctionDetailsforBidder.fxml`
- UI chart seller: `client/src/main/resources/fxml/ActionsScene/AuctionDetailsforSeller.fxml`
- Controller chart: `AuctionDetailsforBidderController.java`, `AuctionDetailsforSellerController.java`
- API lấy history: `ClientProtocolHandler.java`, `ServerProtocolHandler.java`
- Backend history: `server/src/main/java/com/auction/server/service/BidService.java`, `server/src/main/java/com/auction/server/dao/BidDAO.java`

---

## 17. Tính năng sáng tạo khác — 0.5 điểm

**Nên nói nếu cần:**

> Ngoài 3 mục nâng cao trong barem, nhóm còn có wallet transaction, settlement sau khi auction kết thúc, dashboard theo 3 role, admin quản lý user/category, fat JAR chạy độc lập, diagram tài liệu và database issues log để theo dõi lỗi đã xử lý.

**Code chứng minh:**

- Wallet UI: `Wallet1Controller.java`, `Wallet2Controller.java`, `DepositActionController.java`, `WithdrawActionController.java`
- Settlement: `AuctionScheduler.java`, `AuctionService.java`, `UserDAO.java`
- Admin: `AdminDashboardController.java`, `UserManagementController.java`, `CategoryManagementController.java`
- Docs: `docs/`, `MD_tracker/DATABASE_ISSUES_LOG.md`, `MD_tracker/CODEBASE_GUIDE_*.md`

---

# VI. Câu hỏi thầy có thể hỏi

## OOP / Domain

1. Vì sao `User` và `Item` cần cây kế thừa?
2. Nếu thêm loại sản phẩm mới thì sửa ở đâu?
3. Vì sao rule bid nên nằm ở backend/model, không nằm ở UI?
4. `AuctionSession` giữ những thông tin gì?
5. `AuctionStatus` dùng để điều khiển flow như thế nào?

## Concurrency / Realtime

1. Nếu 2 người đặt giá cùng lúc thì tránh race condition ra sao?
2. Vì sao lock theo `sessionId` tốt hơn lock toàn service?
3. `ClientBroadcastHub` hoạt động như Observer như thế nào?
4. Push message khác response message ở điểm nào?
5. Vì sao JavaFX update từ push cần `Platform.runLater()`?

## Database / Architecture

1. Vì sao cần DAO layer?
2. Vì sao client không được query database trực tiếp?
3. Transaction duplicate có thể xảy ra khi nào?
4. CI dùng H2/Mockito để giải quyết vấn đề gì?
5. Maven multi-module có lợi gì?

## UI / Demo

1. FXML liên kết controller bằng gì?
2. Nếu đổi sai `fx:id` thì lỗi gì?
3. Controller gọi API ở đâu?
4. Bid history chart lấy dữ liệu từ đâu?
5. Khi anti-sniping gia hạn, UI biết bằng cách nào?

---

# VII. Câu trả lời nhanh nếu thầy hỏi tổng kết

## Điểm mạnh nhất của project?

> Điểm mạnh là hệ thống không chỉ có CRUD mà có logic đấu giá thực tế: concurrent bidding an toàn, realtime push, auto-bidding, anti-sniping, chart lịch sử giá, test và CI/CD.

## Phần khó nhất?

> Khó nhất là đồng bộ concurrency và realtime. Server phải xử lý nhiều bid cùng lúc an toàn, lưu DB đúng, sau đó broadcast cho mọi client; client lại phải phân biệt response thường và push message để update UI đúng thread.

## Hạn chế hiện tại?

> Đây là project mô phỏng nên chưa có payment gateway thật và chưa scale nhiều server instance. Tuy nhiên trong phạm vi bài tập, các chức năng chính và nâng cao đều có code, demo được và có test/CI hỗ trợ.

---

**File liên quan nên mở cùng:**

- `README.md`
- `MD_tracker/CODEBASE_GUIDE_HAI.md`
- `MD_tracker/CODEBASE_GUIDE_DUY.md`
- `MD_tracker/CODEBASE_GUIDE_HOANG.md`
- `MD_tracker/CODEBASE_GUIDE_GIANG.md`
- `MD_tracker/DATABASE_ISSUES_LOG.md`
