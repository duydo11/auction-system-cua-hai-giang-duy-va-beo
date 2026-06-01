# Progress Track - Đồng bộ auction/admin/wallet - 27/05/2026

## Mục tiêu

Theo dõi các thay đổi để ổn định phần database/network/UI sync trước khi push remote.

## Trạng thái hiện tại

- Đã merge code mới từ Giang vào local và push lần trước.
- Đã audit sâu lỗi admin trống, bidder loading, add product fail.
- Đã thêm smoke test backend/protocol để không sửa mù theo UI.
- Đã compile/install pass.
- Đã smoke test pass:
  - `SMOKE_TEST_PASS activeAuctions=9 allAuctions=18 users=10`

## Các lỗi thật đã phát hiện

### Add Product fail

- Lỗi backend/DB thật:
  - `Duplicate entry '10' for key 'items.PRIMARY'`
- Nguyên nhân:
  - DB schema cũ dùng `id VARCHAR`.
  - Code cấp ID bằng `MAX(id) + 1` nên MySQL so sánh theo chuỗi.
  - Có thể cấp lại ID đã tồn tại.

### Admin dashboard trống/chậm

- `AdminDashboardController` trước đó gần như chỉ navigation.
- Các số `Total Users`, `Total Auctions` là text hardcode trong FXML.
- DAO constructor chạy migration/ALTER nhiều lần làm admin/category load chậm.

### Role bidder/seller không bền vững

- UI switch bidder/seller chỉ đổi object trong memory.
- Khi user bidder tạo auction ở seller mode, DB có thể chưa có row trong `sellers`.
- Điều này có thể làm create item/session fail hoặc seller history lệch.

## Các thay đổi đã làm

### Backend/DB

- Sửa cấp ID ở:
  - `UserDAO`
  - `ItemDAO`
  - `AuctionSessionDAO`
  - `BidDAO`
- Dùng `MAX(CAST(id AS UNSIGNED)) + 1` để tránh duplicate với cột `VARCHAR`.
- `UserDAO` chỉ chạy migration/seed một lần để giảm chậm.
- Thêm `ensureSellerRole(userId)` và `ensureBidderRole(userId)`.
- Khi `CREATE_AUCTION_REQUEST`, server tự đảm bảo seller role row tồn tại.
- `ItemDAO` không nuốt lỗi SQL nữa, để lỗi thật hiện lên khi create/delete fail.

### Smoke test

- Thêm `ProtocolSmokeTest` để test không qua UI:
  - login/register smoke user
  - create auction category `Others`
  - query active auctions
  - query all auctions
  - query all users

### Admin UI

- Admin dashboard load số liệu thật từ server:
  - total users
  - total auctions
- Category management dùng UI text English:
  - `Loading auctions...`
  - `Loaded X auctions`
  - `Delete`
- Category management có trạng thái loading/error rõ hơn.

### Bidder UI

- Bidder dashboard tránh refresh chồng chéo khi network/DB chậm.
- Giảm khả năng UI bị kẹt ở `Loading active auctions...`.

### Add Product

- `Others` trong UI đã được `ItemFactory` support.
- Comment vẫn bằng tiếng Việt, còn message/UI text giữ English cho đồng bộ giao diện.

## Verification

- `mvn -q -DskipTests install`: PASS
- `ProtocolSmokeTest`: PASS

Kết quả smoke test cuối:

```text
SMOKE_TEST_PASS activeAuctions=9 allAuctions=18 users=10
```

## Việc còn lại

- [x] Compile/install pass.
- [x] Smoke test pass.
- [x] Cập nhật tracker.
- [ ] Commit và push.

## Ghi chú

Không push thẳng khi remote vừa có thay đổi. Luôn fetch/review/merge/compile/smoke test trước để tránh đè code của người khác.
