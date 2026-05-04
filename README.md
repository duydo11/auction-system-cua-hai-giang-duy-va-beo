# Kế hoạch phân chia công việc BTL LTNC (Tuần 8-15)

Tài liệu này được viết theo góc nhìn "từ đầu dự án chưa làm gì", mục tiêu là để cả nhóm thống nhất vai trò, đầu việc, mốc kiểm tra và quy tắc phối hợp trước khi code.

## 1) Mục tiêu chung của nhóm

- Xây dựng hệ thống đấu giá online theo kiến trúc `client-server-shared`.
- Đảm bảo các chức năng bắt buộc: quản lý user, tạo phiên đấu giá, đặt giá, realtime update, xử lý lỗi, unit test, CI.
- Đảm bảo từng thành viên hiểu toàn bộ luồng chính để đi bảo vệ không bị đứt mạch.

## 2) Thành viên và trách nhiệm bản chất

| Thành viên | Vai trò bản chất | Mục tiêu kỹ thuật chính |
|---|---|---|
| Hải | Cái Não (Backend Logic) | Viết và giữ "luật chơi" đấu giá đúng nghiệp vụ |
| Giang | Gương mặt (Frontend) | Làm UI/UX JavaFX rõ ràng, thao tác mượt, dễ demo |
| Hoàng | Cánh tay (Networking) | Kết nối UI và logic qua socket ổn định, ít lỗi |
| Duy | Cảnh sát (Concurrency & Quality) | Chống race condition, test và CI xanh |

---

## 3) Phân rã công việc chi tiết theo từng thành viên

## 3.1 Hải - Backend Logic (Cái Não)

### Trách nhiệm cốt lõi
- Thiết kế model domain: `User`, `Item`, `AuctionSession`, `Bid`.
- Thiết kế rule nghiệp vụ đấu giá để các bên khác bám theo.
- Định nghĩa rõ đầu vào/đầu ra của các API nghiệp vụ cho Hoàng và Giang.

### Deliverables bắt buộc
- Bộ hàm nghiệp vụ:
  - `checkValidBid(...)`
  - `updateWinner(...)`
  - `closeAuctionIfExpired(...)`
- Bộ lỗi nghiệp vụ rõ ràng:
  - bid thấp hơn giá hiện tại
  - phiên đã đóng
  - user không đúng role
- Mapping chuẩn sang response cho tầng networking.

### Việc theo tuần (8-15)
- Tuần 8:
  - Chốt domain model, chuẩn hóa ràng buộc dữ liệu.
  - Viết rule đấu giá và custom exception cơ bản.
- Tuần 9:
  - Bàn giao contract nghiệp vụ cho Hoàng (message type + payload mẫu).
  - Viết tài liệu rule bằng markdown ngắn.
- Tuần 10:
  - Tích hợp với server service/DAO.
  - Đảm bảo rule giữ nguyên khi kết nối socket vào.
- Tuần 11-12:
  - Sửa bug edge cases khi nhiều client thao tác.
  - Chốt luồng đóng phiên và xác định winner.
- Tuần 13-14:
  - Refactor nhẹ, giảm code lặp.
  - Hỗ trợ Duy viết test case nghiệp vụ.
- Tuần 15:
  - Chuẩn bị phần thuyết minh "vì sao luật nghiệp vụ viết như vậy".

### Bàn giao cho đồng đội
- Cho Hoàng:
  - Danh sách case thành công/thất bại và thông điệp lỗi tương ứng.
- Cho Giang:
  - Danh sách thông báo cần hiển thị cho người dùng.
- Cho Duy:
  - Danh sách case critical để viết test.

---

## 3.2 Giang - Frontend JavaFX (Gương mặt)

### Trách nhiệm cốt lõi
- Thiết kế UI bằng SceneBuilder/FXML.
- Viết controller nhận input, validate cơ bản, gọi xuống networking.
- Hiển thị dữ liệu và lỗi rõ ràng cho user.

### Deliverables bắt buộc
- Màn hình chính:
  - Login/Register
  - Bidder Dashboard, Items, My Bids
  - Seller Dashboard
- Card hiển thị sản phẩm/lịch sử bid có thể gắn dữ liệu thật.
- Điều hướng scene ổn định, không crash khi thiếu dữ liệu.

### Việc theo tuần (8-15)
- Tuần 8:
  - Dựng skeleton giao diện và navigation.
  - Chốt style cơ bản, không sa đà animation.
- Tuần 9:
  - Làm form input và validate tối thiểu.
  - Dùng dữ liệu mock để test flow màn hình.
- Tuần 10:
  - Tích hợp call protocol của Hoàng.
  - Chuyển từ mock sang data thật từng màn.
- Tuần 11-12:
  - Bổ sung trạng thái lỗi thân thiện: offline, timeout, input sai.
  - Đảm bảo màn không trắng khi server lỗi.
- Tuần 13-14:
  - Polish UI, sửa spacing/font/icon cho demo.
  - Đồng bộ text thông báo với rule từ Hải.
- Tuần 15:
  - Chuẩn bị demo thao tác 1 vòng end-to-end.

### Bàn giao cho đồng đội
- Cho Hoàng:
  - Danh sách field/input mỗi request cần gửi.
- Cho Hải:
  - Các case user thao tác thực tế để kiểm tra rule.
- Cho Duy:
  - Danh sách screen cần test tay trước khi merge.

---

## 3.3 Hoàng - Networking Socket (Cánh tay)

### Trách nhiệm cốt lõi
- Xây dựng luồng giao tiếp `SocketClient <-> SocketServer`.
- Chuẩn hóa request/response object (`Message`, `MessageType`).
- Đảm bảo lỗi mạng không làm app chết im lặng.

### Deliverables bắt buộc
- `SocketServer` nhận nhiều client.
- `SocketClient` có timeout/read loop ổn định.
- `ClientProtocolHandler` để controller dùng dễ.
- Cơ chế xử lý lỗi tối thiểu:
  - timeout
  - mất kết nối
  - retry nhẹ (nếu cần)

### Việc theo tuần (8-15)
- Tuần 8:
  - Chốt protocol message và danh sách `MessageType`.
  - Viết skeleton client/server socket.
- Tuần 9:
  - Kết nối login/register/create/place bid end-to-end.
  - Bàn giao API gọi cho Giang.
- Tuần 10:
  - Thêm correlationId, phân luồng response/push.
  - Tích hợp realtime update cho màn đấu giá.
- Tuần 11-12:
  - Harden runtime: timeout, fail-fast, log lỗi.
  - Giảm treo UI khi server unavailable.
- Tuần 13-14:
  - Rà socket edge cases, giữ log gọn dễ debug.
  - Tối ưu mức cơ bản, không over-engineer reconnect.
- Tuần 15:
  - Chuẩn bị thuyết minh luồng "request -> server -> response/push".

### Bàn giao cho đồng đội
- Cho Giang:
  - Hàm protocol cụ thể cho từng thao tác UI.
- Cho Hải:
  - Cách đóng gói payload để service xử lý đúng.
- Cho Duy:
  - Danh sách lỗi mạng cần test regression.

---

## 3.4 Duy - Concurrency, Test, CI (Cảnh sát)

### Trách nhiệm cốt lõi
- Đảm bảo đặt giá đồng thời không sai dữ liệu.
- Xây unit test cho logic quan trọng.
- Duy trì pipeline CI chạy build/test ổn định.

### Deliverables bắt buộc
- Critical section cho đặt giá đồng thời (`synchronized` hoặc lock hợp lý).
- Unit test cho case quan trọng:
  - bid hợp lệ
  - bid không hợp lệ
  - phiên đóng
  - đồng thời nhiều bidder
- GitHub Actions chạy:
  - compile
  - test

### Việc theo tuần (8-15)
- Tuần 8:
  - Xác định điểm race condition trong luồng bid.
- Tuần 9:
  - Bổ sung lock/sync tối thiểu, không phá kiến trúc.
  - Tạo test skeleton.
- Tuần 10:
  - Viết test cho rule nghiệp vụ chính từ Hải.
  - Thiết lập workflow CI cơ bản.
- Tuần 11-12:
  - Chạy test đồng thời nhiều vòng, fix flaky test.
  - Thêm check cho lỗi phổ biến.
- Tuần 13-14:
  - Dọn test data, làm báo cáo test ngắn.
  - Kiểm tra merge conflict định kỳ.
- Tuần 15:
  - Chuẩn bị phần trình bày "chất lượng code và độ tin cậy".

### Bàn giao cho đồng đội
- Cho cả nhóm:
  - checklist trước merge (build, test, chạy tay).
- Cho Hải/Hoàng:
  - báo lỗi concurrency/network có thể tái hiện.
- Cho Giang:
  - checklist UI regression ngắn.

---

## 4) Quy tắc phối hợp nhóm (bắt buộc)

## 4.1 Quy tắc branch/PR
- Mỗi người làm trên branch riêng: `hai`, `giang`, `hoang`, `duy`.
- Không push thẳng `main`.
- Mọi merge đi qua PR, có ít nhất 1 người review.

## 4.2 Quy tắc commit
- Commit nhỏ theo tính năng, message rõ nghĩa.
- Không dồn 1 commit lớn cuối kỳ.
- Mỗi commit phải build được ở mức tối thiểu.

## 4.3 Quy tắc bàn giao
- Mỗi task xong phải ghi:
  - file đã sửa
  - ảnh hưởng module nào
  - cách test nhanh
- Không bàn giao kiểu "xong rồi nhưng chưa biết chạy".

## 4.4 Quy tắc xử lý conflict
- Conflict UI: Giang quyết định chính, hỏi thêm Hoàng nếu có call network.
- Conflict logic: Hải quyết định chính, Duy review test.
- Conflict socket/protocol: Hoàng quyết định chính, Hải xác nhận nghiệp vụ.

---

## 5) Kế hoạch tích hợp theo mốc

- Mốc A (cuối tuần 9): chạy được login/register qua socket.
- Mốc B (cuối tuần 10): tạo phiên + đặt giá + xem danh sách.
- Mốc C (cuối tuần 12): realtime + concurrency cơ bản + test chạy.
- Mốc D (cuối tuần 14): demo ổn định, README hoàn chỉnh.
- Mốc E (tuần 15): rehearsal thuyết trình + demo cuối.

---

## 6) Checklist "Definition of Done" cho từng tính năng

Một tính năng chỉ được coi là xong khi đủ 6 điều:

1. Code chạy được trên máy local.
2. Không crash khi nhập dữ liệu xấu.
3. Lỗi hiển thị đủ để user hiểu.
4. Có test hoặc manual test steps đính kèm.
5. Được ít nhất 1 thành viên khác review.
6. Merge không làm hỏng luồng cũ.

---

## 7) Rủi ro chính và phương án dự phòng

- Rủi ro 1: Lệch contract giữa UI và network.
  - Giảm thiểu: chốt payload mẫu sớm, dùng chung tài liệu message.
- Rủi ro 2: Đua tiến độ UI khiến logic chưa ổn.
  - Giảm thiểu: ưu tiên luồng chạy được trước, polish sau.
- Rủi ro 3: Race condition lúc đặt giá đồng thời.
  - Giảm thiểu: lock tại critical path + test concurrency.
- Rủi ro 4: Demo fail vì môi trường.
  - Giảm thiểu: chuẩn bị script chạy nhanh và dữ liệu seed.

---

## 8) Kịch bản họp nhóm mỗi tuần (gợi ý)

Mỗi buổi 30-45 phút, theo thứ tự:

1. Mỗi người báo 3 ý:
   - Đã làm gì
   - Đang vướng gì
   - Cần ai hỗ trợ
2. Chốt task tuần tới theo đúng vai trò.
3. Chốt mốc tích hợp giữa 2 module.
4. Ghi biên bản ngắn vào tài liệu nhóm.

---

## 9) Tuyên bố phạm vi để đi chấm điểm

Nhóm ưu tiên:
- Chạy end-to-end ổn định.
- Demo mượt luồng chính.
- Giải thích rõ lý do thiết kế.
- Thành thật về giới hạn hiện tại.

Nhóm không ưu tiên:
- Tính năng nâng cao chưa chắc chắn.
- Refactor lớn sát deadline.
- Tối ưu sớm gây rủi ro.
