# Hướng dẫn duy nhất cho Giang — UI/FXML, Data Binding, Realtime và Demo

> File này là tài liệu duy nhất Giang cần đọc để catch up phần UI.
> Mục tiêu: Giang chỉ chỉnh giao diện/FXML cho đẹp và đồng bộ, không đổi tên `fx:id` hoặc handler đang được Java controller dùng.

---

## 1. Giang đang phụ trách phần nào?

Giang phụ trách phần **JavaFX UI/FXML**:

```text
client/src/main/resources/fxml/
client/src/main/resources/png/
client/src/main/java/com/auction/client/controller/
```

Luồng UI hiện tại:

```text
FXML file
  ↓ fx:controller
Java Controller
  ↓ ClientProtocolHandler
SocketClient
  ↓ Message protocol
ServerProtocolHandler
  ↓ Service
DAO
  ↓
Database
```

Nói đơn giản:

- FXML chỉ là phần vẽ màn hình.
- Controller là phần đổ dữ liệu thật vào FXML.
- Backend trả `AuctionSession`, `Bid`, `User`, `Transaction`.
- Controller lấy dữ liệu đó rồi set vào `Label`, `VBox`, `LineChart`, card.

---

## 2. Nguyên tắc quan trọng khi sửa FXML

> [!IMPORTANT]
> Không đổi tên các `fx:id` dưới đây nếu không sửa controller tương ứng.

Nếu đổi sai `fx:id`, JavaFX sẽ bị lỗi kiểu:

```text
NullPointerException
LoadException
Cannot resolve onAction/onMouseClicked
```

Giang được sửa:

- màu nền,
- font,
- spacing,
- kích thước,
- vị trí layout,
- ảnh,
- bo góc,
- style CSS inline.

Giang không nên sửa nếu chưa báo team:

- `fx:id`,
- `fx:controller`,
- `onAction`,
- `onMouseClicked`,
- đường dẫn FXML card như `/fxml/Card/ProductCard.fxml`.

---

## 3. Các màn hình đã được backend đổ dữ liệu thật

### BidderDashboard.fxml

Controller: `BidderDashboardController`

Các field quan trọng:

```java
@FXML private Label lblUsername;
@FXML private HBox container1;
@FXML private HBox container2;
```

Chức năng:

- Hiển thị tên user đang login.
- Gọi backend lấy danh sách auction thật.
- Tự load `ProductCard.fxml` vào `container1`, `container2`.
- Lắng nghe realtime push để card tự đổi giá/status/time.

Luồng:

```text
initialize()
  → loadActiveAuctions()
  → protocol.getActiveAuctions()
  → mỗi AuctionSession tạo 1 ProductCard
  → ProductCardController.setAuctionSession(session)
```

Realtime:

```text
Server broadcast AUCTION_UPDATED_PUSH
  → SocketClient
  → RealtimeAuctionBus
  → BidderDashboardController listener
  → ProductCardController re-bind
```

---

### ProductCard.fxml

Controller: `ProductCardController`

Các `fx:id` bắt buộc:

```xml
<Label fx:id="lblStatus" />
<Label fx:id="lblItemName" />
<HBox fx:id="lblCategory" />
<Label fx:id="lblCurrentPrice" />
<Button fx:id="btnViewDetails" onAction="#handleViewDetails" />
```

Chức năng:

- Hiển thị tên sản phẩm.
- Hiển thị giá hiện tại.
- Hiển thị status: Coming / Live / Ended / Canceled.
- Hiển thị category bằng chữ cái/màu.
- Click `View Details` mở `AuctionDetailsforBidder.fxml`.
- Có method `updatePrice()` để realtime đổi giá ngay trên card.

Gợi ý UI đồng bộ:

| Thành phần | Màu gợi ý |
|---|---|
| Coming | vàng nhạt |
| Live | xanh lá |
| Ended | đỏ/hồng nhạt |
| Canceled | xám |
| Nút View Details | tím/xanh đồng bộ dashboard |

---

### AuctionDetailsforBidder.fxml

Controller: `AuctionDetailsforBidderController`

Các `fx:id` bắt buộc:

```xml
<Label fx:id="lblStatus" />
<Label fx:id="lblItemName" />
<Label fx:id="lblItemDetails" />
<Label fx:id="lblSeller" />
<TextField fx:id="txtBidAmount" />
<HBox fx:id="btnPlaceBid" onMouseClicked="#handlePlaceBid" />
<CheckBox fx:id="chboxAutobid" onAction="#hanldeAutoBid" />
<VBox fx:id="paneAutobid" />
<TextField fx:id="txtMaxBidAmount" />
<TextField fx:id="txtIncrementAmount" />
<Label fx:id="lblCurrentPrice" />
<Label fx:id="lblCurrentPriceSmall" />
<Label fx:id="lblStartPrice" />
<Label fx:id="lblTimeH" />
<Label fx:id="lblTimem" />
<Label fx:id="lblTimes" />
<Label fx:id="lblCurrentBids" />
<Label fx:id="lblWarning" />
<LineChart fx:id="lcPriceHistory" />
<VBox fx:id="containerBidHistory" />
```

Chức năng:

- Đổ thông tin phiên đấu giá thật.
- Đặt giá thật qua backend.
- Đăng ký/hủy auto-bid.
- Hiển thị lịch sử bid bằng card.
- Vẽ biểu đồ giá bằng `LineChart`.
- Countdown theo `endTime`.
- Hiển thị cảnh báo anti-sniping:
  - còn dưới 5 phút: nhắc chuẩn bị đặt giá,
  - còn dưới 30 giây: cảnh báo bid mới có thể gia hạn,
  - khi server gia hạn: hiện thông báo auction extended.
- Lắng nghe realtime push để tự refresh giá, chart, bid history, countdown.

Luồng đặt giá:

```text
User nhập txtBidAmount
  → click btnPlaceBid
  → handlePlaceBid()
  → protocol.placeBidOrError(sessionId, userId, amount)
  → server BidService.placeBid()
  → lưu Bid vào DB
  → broadcast AUCTION_UPDATED_PUSH
  → UI tự refresh
```

---

### AuctionDetailsforSeller.fxml

Controller: `AuctionDetailsforSellerController`

Các `fx:id` bắt buộc:

```xml
<Label fx:id="lblStatus" />
<Label fx:id="lblItemName" />
<Label fx:id="lblItemDetails" />
<Label fx:id="lblSeller" />
<Label fx:id="lblCurrentPrice" />
<Label fx:id="lblStartPrice" />
<Label fx:id="lblTimeH" />
<Label fx:id="lblTimem" />
<Label fx:id="lblTimes" />
<Label fx:id="lblCurrentBids" />
<Label fx:id="lblWarning" />
<LineChart fx:id="lcPriceHistory" />
<VBox fx:id="containerBidHistory" />
<HBox fx:id="containerResult" />
```

Chức năng:

- Seller xem thông tin phiên của mình.
- Xem realtime bid count/current price/chart.
- Xem cảnh báo anti-sniping.
- Khi phiên kết thúc:
  - nếu có winner: load `AuctionResult1Card.fxml`,
  - nếu không có bid: load `AuctionResult2Card.fxml`.

---

### BidderHistoryCard.fxml

Controller: `BidderHistoryCardController`

Các `fx:id`:

```xml
<Label fx:id="lblBidder" />
<Label fx:id="lblBidAmount" />
<Label fx:id="lblBidDay" />
<Label fx:id="lblBidTime" />
```

Chức năng:

- Mỗi card là 1 lần đặt giá.
- Controller nhận `Bid` rồi hiển thị:
  - username người đặt,
  - số tiền,
  - ngày,
  - giờ.

---

### Wallet1.fxml và Wallet2.fxml

Controllers:

- `Wallet1Controller` cho Bidder.
- `Wallet2Controller` cho Seller.

Các `fx:id`:

```xml
<Label fx:id="lblTotalBalance" />
<Label fx:id="lblAvailabe" />
<Label fx:id="lblReserved" />
<VBox fx:id="containerTrans" />
```

Chức năng:

- Lấy user hiện tại từ server.
- Hiển thị tổng tiền, tiền khả dụng, tiền reserved.
- Load `TransHisCard.fxml` theo transaction history.
- Sau deposit/withdraw, server cập nhật balance và lưu transaction.
- Khi auction thành công, scheduler tạo transaction cho buyer/seller.

---

### DepositAction.fxml và WithdrawAction.fxml

Controllers:

- `DepositActionController`
- `WithdrawActionController`

Các `fx:id`:

```xml
<TextField fx:id="txtDepositAmount" />
<HBox fx:id="btnConfirm" onMouseClicked="#handleConfirm" />

<TextField fx:id="txtWithdrawAmount" />
<HBox fx:id="btnConfirm" onMouseClicked="#handleConfirm" />
```

Chức năng:

- Validate số tiền nhập.
- Gọi backend update user balance.
- Lưu transaction:
  - `DEPOSIT`,
  - `WITHDRAW`.

---

## 4. Bid History Chart hoạt động như nào?

Trong detail controller:

```java
List<Bid> bids = protocol.getBidHistory(session.getId());
XYChart.Series<String, Number> series = new XYChart.Series<>();
series.getData().add(new XYChart.Data<>("Start", session.getStartingPrice()));
for (Bid bid : bids) {
    series.getData().add(new XYChart.Data<>(timeLabel, bid.getAmount()));
}
lcPriceHistory.getData().add(series);
```

Ý nghĩa:

- Điểm đầu là giá khởi điểm.
- Mỗi bid là 1 điểm trên chart.
- Trục X là thời gian đặt giá.
- Trục Y là số tiền.
- Khi server push bid mới, controller gọi lại `loadBidHistoryChart()` nên chart refresh realtime.

---

## 5. Anti-sniping UI hoạt động như nào?

Backend đã có logic trong `BidService`:

```text
Nếu bid được đặt trong 30 giây cuối
  → endTime += 60 giây
  → lưu session
  → broadcast update cho client
```

UI đã có:

```xml
<Label fx:id="lblWarning" />
```

Controller sẽ set text:

- `< 5 phút`: `Auction ending soon...`
- `< 30 giây`: cảnh báo anti-sniping.
- Server gia hạn: `Anti-sniping activated: auction extended by server.`

Giang chỉ cần style label này cho nổi bật, ví dụ:

```xml
<Label fx:id="lblWarning"
       style="-fx-text-fill: #b45309; -fx-font-weight: bold;"
       wrapText="true" />
```

---

## 6. Một phiên đấu giá hoàn chỉnh từ đầu đến cuối

```text
Seller login
  → Seller tạo item/auction
  → Server lưu item + auction_sessions
  → Broadcast AUCTION_CREATED_PUSH
  → BidderDashboard thấy card mới

Bidder login
  → mở ProductCard detail
  → đặt bid
  → Server lưu bids + update current_price/winner
  → Nếu bid trong 30s cuối: gia hạn end_time
  → Broadcast AUCTION_UPDATED_PUSH
  → Dashboard/detail/chart/history cập nhật realtime

Auction hết giờ
  → AuctionScheduler scan
  → status = FINISHED
  → tạo transaction buyer/seller
  → broadcast CLOSE_AUCTION_PUSH
  → Seller detail hiện result card
  → Wallet hiện transaction history
```

---

## 7. Checklist để Giang tự test UI

### Bidder flow

- [ ] Login bidder.
- [ ] Vào dashboard thấy product cards có dữ liệu thật.
- [ ] Bấm View Details.
- [ ] Thấy item name/details/seller/current price/start price.
- [ ] Thấy countdown chạy.
- [ ] Đặt bid lớn hơn current price.
- [ ] Bid history card xuất hiện.
- [ ] Line chart có thêm điểm mới.
- [ ] Mở 2 client, client còn lại cũng tự update giá.

### Anti-sniping

- [ ] Tạo auction sắp hết giờ.
- [ ] Bid trong 30s cuối.
- [ ] Countdown được gia hạn.
- [ ] `lblWarning` hiện thông báo extended.

### Seller flow

- [ ] Login seller.
- [ ] Mở detail auction của mình.
- [ ] Thấy current price/bid count/chart realtime.
- [ ] Chờ phiên kết thúc.
- [ ] Có winner thì hiện `AuctionResult1Card`.
- [ ] Không có bid thì hiện `AuctionResult2Card`.

### Wallet flow

- [ ] Deposit tiền.
- [ ] Wallet balance tăng.
- [ ] Transaction history có dòng Deposit.
- [ ] Withdraw tiền.
- [ ] Wallet balance giảm.
- [ ] Transaction history có dòng Withdraw.
- [ ] Auction kết thúc có winner thì buyer/seller có transaction đấu giá.

---

## 8. File nào Giang nên chỉnh nếu muốn làm UI đẹp hơn?

| Màn hình | File FXML |
|---|---|
| Dashboard bidder | `fxml/BidderScene/BidderDashboard.fxml` |
| Card sản phẩm | `fxml/Card/ProductCard.fxml` |
| Bidder detail | `fxml/ActionsScene/AuctionDetailsforBidder.fxml` |
| Seller detail | `fxml/ActionsScene/AuctionDetailsforSeller.fxml` |
| Bid history card | `fxml/Card/BidderHistoryCard.fxml` |
| Wallet bidder | `fxml/BidderScene/Wallet1.fxml` |
| Wallet seller | `fxml/SellerScene/Wallet2.fxml` |
| Transaction card | `fxml/Card/TransHisCard.fxml` |
| Result có winner | `fxml/Card/AuctionResult1Card.fxml` |
| Result không winner | `fxml/Card/AuctionResult2Card.fxml` |

---

## 9. Lỗi thường gặp

### `NullPointerException` khi mở màn

Nguyên nhân thường là thiếu `fx:id`.

Cách sửa:

- Mở controller.
- Tìm field `@FXML private ... name;`
- FXML phải có `fx:id="name"` đúng y hệt.

### Click button không chạy

Nguyên nhân thường là sai handler.

Ví dụ đúng:

```xml
<HBox onMouseClicked="#handlePlaceBid" />
<Button onAction="#handleViewDetails" />
```

Tên sau `#` phải tồn tại trong controller.

### Chart không hiện

Kiểm tra:

```xml
<LineChart fx:id="lcPriceHistory">
```

Và auction phải có bid history.

### Realtime không update

Kiểm tra:

- server đang chạy,
- client còn connected,
- `SocketClient` nhận push,
- controller chưa bị cleanup,
- đang dùng đúng `AuctionSession.id`.

---

## 10. Kết luận cho Giang

Hiện tại phần Java controller đã làm nhiệm vụ:

- data binding thật,
- protocol backend,
- realtime listener,
- bid chart,
- anti-sniping warning,
- wallet transaction,
- auction result.

Giang chỉ cần tập trung:

1. làm FXML đồng bộ style,
2. giữ nguyên `fx:id`/handler,
3. test theo checklist trên,
4. không cần tự viết backend logic nữa.
