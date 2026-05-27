# Progress Track - Đồng bộ auction/admin/wallet - 27/05/2026

## Mục tiêu

Theo dõi các thay đổi đang làm để ổn định phần database/network/UI sync trước khi push remote.

## Trạng thái hiện tại

- Đã fetch code mới từ `origin/main` của Giang.
- Đã tạo nhánh backup local: `backup-before-merge-giang-remote`.
- Đang merge thay đổi remote vào local, chưa push.
- Đã resolve conflict ở:
  - `BidderDashboardController.java`
  - `ItemsController.java`

## Các thay đổi local đã giữ lại

### Cache và đồng bộ auction

- Tách `AuctionCache` thành `activeAuctions` và `allAuctions`.
- Bidder dashboard/items chỉ dùng active cache.
- Seller/admin/My Listings dùng all cache để giữ lịch sử.
- Bidder dashboard có refresh nền + polling nhẹ để tránh miss realtime push.

### Admin

- Ban user theo kiểu ban mềm bằng `is_banned`, không xóa user.
- User bị ban không đăng nhập được và nhận lỗi: `bạn đã bị admin ban`.
- Admin có nút xóa sản phẩm trong category management.
- Server xóa item sẽ xóa bid/session liên quan trước.

### Wallet/deposit

- Deposit không cộng tiền local trước.
- Server update thành công mới refresh lại user trong `SessionContext`.

## Các thay đổi của Giang đã được giữ khi merge

- UI/controller mới cho bidder items theo category tab.
- Các chỉnh sửa FXML ở wallet/product card/details/listing.
- Các controller UI liên quan:
  - `AuctionDetailsforBidderController.java`
  - `AuctionDetailsforSellerController.java`
  - `MyBidsController.java`
  - `ProductCardController.java`
  - `Wallet1Controller.java`
  - `Wallet2Controller.java`
  - `ShippingController.java`

## Việc cần làm tiếp

- [x] Compile sau merge.
- [x] Nếu compile lỗi, sửa conflict logic/UI còn sót.
- [x] Commit merge local.
- [x] Push sau khi compile pass.

## Ghi chú

Không push thẳng khi remote vừa có thay đổi. Luôn fetch/review/merge/compile trước để tránh đè code của người khác.
