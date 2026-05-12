# 🔌 Implementation Plan — Hoàng (Networking) — Phase 1-3

## Mục Tiêu Chung

Hoàn thành **8 task** trong Phase 1-3 để bổ sung protocol cho các tính năng mới:
- Phase 1: Thêm MessageType mới + cập nhật handler (3 tasks)
- Phase 2: Xử lý push mới + error propagation (2 tasks)
- Phase 3: Protocol nâng cao + harden network (2 tasks)
- Phase 4: Viết docs (1 task)

**Thời gian ước tính**: ~12 giờ

---

## 📊 Phân Tích Hiện Trạng

### MessageType hiện có (45 types):
```java
// Auth: LOGIN, REGISTER, LOGOUT (6 types)
// Auction: VIEW_AUCTIONS, CREATE_AUCTION, AUCTION_DETAILS (6 types)
// Bidding: PLACE_BID, GET_BIDS (4 types)
// Push: AUCTION_UPDATED_PUSH, AUCTION_CREATED_PUSH (2 types)
// User: GET_USER_INFO, UPDATE_USER (4 types)
// Category: GET_CATEGORIES (2 types)
// Error: ERROR, SUCCESS, ACK (3 types)
```

### Thiếu gì?
Theo WORK_ASSIGNMENT.md, cần thêm:
1. **UPDATE_ITEM** / **DELETE_ITEM** (Seller quản lý sản phẩm)
2. **CLOSE_AUCTION_PUSH** (auto-close phiên)
3. **AUCTION_EXTENDED_PUSH** (anti-snipe)
4. **REGISTER_AUTO_BID** / **CANCEL_AUTO_BID** (auto-bidding)
5. **BAN_USER** / **GET_ALL_USERS** (admin)

---

## 🎯 Phase 1 (Ngày 1-3): Fix Bắt Buộc

### ✅ Task P1-N1: Thêm MessageType Mới

**File**: `shared/src/main/java/com/auction/shared/protocol/MessageType.java`

**Thêm 10 MessageType**:
```java
// Item Management (Seller)
UPDATE_ITEM_REQUEST,
UPDATE_ITEM_RESPONSE,
DELETE_ITEM_REQUEST,
DELETE_ITEM_RESPONSE,

// Auto-close & Anti-snipe Push
CLOSE_AUCTION_PUSH,           // Server → Client: phiên đã đóng
AUCTION_EXTENDED_PUSH,        // Server → Client: phiên gia hạn (anti-snipe)

// Auto-Bidding
REGISTER_AUTO_BID_REQUEST,
REGISTER_AUTO_BID_RESPONSE,
CANCEL_AUTO_BID_REQUEST,
CANCEL_AUTO_BID_RESPONSE,

// Admin
BAN_USER_REQUEST,
BAN_USER_RESPONSE,
GET_ALL_USERS_REQUEST,
GET_ALL_USERS_RESPONSE
```

**Lý do**: Các tính năng mới cần protocol riêng.

---

### ✅ Task P1-N2: Cập Nhật ServerProtocolHandler

**File**: `server/src/main/java/com/auction/server/network/ServerProtocolHandler.java`

**Thêm case mới vào switch**:
```java
case UPDATE_ITEM_REQUEST -> handleUpdateItem(message.getData());
case DELETE_ITEM_REQUEST -> handleDeleteItem(message.getData());
case REGISTER_AUTO_BID_REQUEST -> handleRegisterAutoBid(message.getData());
case CANCEL_AUTO_BID_REQUEST -> handleCancelAutoBid(message.getData());
case BAN_USER_REQUEST -> handleBanUser(message.getData());
case GET_ALL_USERS_REQUEST -> handleGetAllUsers();
```

**Implement handlers**:
```java
private Message handleUpdateItem(Object data) throws Exception {
    // data = Item object
    // Gọi ItemDAO.updateItem() hoặc AuctionService.updateItem()
    // Validate: chỉ seller của item mới được sửa
    // Validate: chỉ sửa được khi phiên chưa RUNNING
}

private Message handleDeleteItem(Object data) throws Exception {
    // data = itemId (int)
    // Gọi ItemDAO.deleteItem()
    // Validate: chỉ seller của item mới được xóa
    // Validate: chỉ xóa được khi phiên chưa RUNNING
}

private Message handleRegisterAutoBid(Object data) throws Exception {
    // data = AutoBidConfig object
    // Gọi AutoBidService.registerAutoBid()
    // Return success/fail
}

private Message handleCancelAutoBid(Object data) throws Exception {
    // data = [sessionId, bidderId]
    // Gọi AutoBidService.cancelAutoBid()
}

private Message handleBanUser(Object data) throws Exception {
    // data = userId (int)
    // Gọi UserService.banUser() hoặc UserDAO.deleteUser()
    // Validate: chỉ admin mới được ban
}

private Message handleGetAllUsers() throws Exception {
    // Gọi UserDAO.getAllUsers()
    // Return List<User>
}
```

**Phụ thuộc**: Hải phải hoàn thành AutoBidService, ItemDAO methods trước.

---

### ✅ Task P1-N3: Cập Nhật ClientProtocolHandler

**File**: `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`

**Thêm methods mới**:
```java
public boolean updateItem(Item item) {
    Message response = send(MessageType.UPDATE_ITEM_REQUEST, item);
    return response != null && response.isSuccess();
}

public String updateItemOrError(Item item) {
    Message response = send(MessageType.UPDATE_ITEM_REQUEST, item);
    if (response != null && response.isSuccess()) return null;
    return lastError(response);
}

public boolean deleteItem(int itemId) {
    Message response = send(MessageType.DELETE_ITEM_REQUEST, itemId);
    return response != null && response.isSuccess();
}

public boolean registerAutoBid(AutoBidConfig config) {
    Message response = send(MessageType.REGISTER_AUTO_BID_REQUEST, config);
    return response != null && response.isSuccess();
}

public boolean cancelAutoBid(int sessionId, int bidderId) {
    Message response = send(MessageType.CANCEL_AUTO_BID_REQUEST, 
        new int[]{sessionId, bidderId});
    return response != null && response.isSuccess();
}

public boolean banUser(int userId) {
    Message response = send(MessageType.BAN_USER_REQUEST, userId);
    return response != null && response.isSuccess();
}

@SuppressWarnings("unchecked")
public List<User> getAllUsers() {
    Message response = send(MessageType.GET_ALL_USERS_REQUEST, null);
    if (response == null || !response.isSuccess()) {
        return Collections.emptyList();
    }
    Object data = response.getData();
    if (data instanceof List<?> list && !list.isEmpty() && 
        list.get(0) instanceof User) {
        return (List<User>) data;
    }
    return Collections.emptyList();
}
```

**Bàn giao cho Giang**: Giang có thể gọi các method này từ UI controller.

---

## 🎯 Phase 2 (Ngày 4-6): Chức Năng Thiếu

### ✅ Task P2-N4: Xử Lý Push Mới

**File**: `client/src/main/java/com/auction/client/network/SocketClient.java`

**Cập nhật `dispatchIncoming()`**:
```java
private void dispatchIncoming(Message incoming) {
    String cid = incoming.getCorrelationId();
    
    // RPC response
    if (cid != null && !cid.isBlank()) {
        CompletableFuture<Message> future = pendingRequests.remove(cid);
        if (future != null) {
            future.complete(incoming);
            return;
        }
    }
    
    // Server push
    MessageType type = incoming.getType();
    if (type == MessageType.AUCTION_UPDATED_PUSH || 
        type == MessageType.AUCTION_CREATED_PUSH ||
        type == MessageType.CLOSE_AUCTION_PUSH ||        // ← MỚI
        type == MessageType.AUCTION_EXTENDED_PUSH) {     // ← MỚI
        RealtimeAuctionBus.dispatch(incoming);
    }
}
```

**Bàn giao cho Giang**: Giang cần subscribe vào `RealtimeAuctionBus` để nhận push mới.

---

### ✅ Task P2-N5: Cải Thiện Error Propagation

**Vấn đề**: Server trả error message nhưng client không hiển thị rõ.

**Giải pháp**:

1. **Server**: Đảm bảo `ServerProtocolHandler` catch exception và trả error message cụ thể:
```java
private Message handlePlaceBid(Object data) throws Exception {
    try {
        // ... logic ...
        boolean success = bidService.placeBid(sessionId, bidderId, bidAmount);
        if (success) {
            // ...
            return new Message(MessageType.PLACE_BID_RESPONSE, "Bid placed successfully");
        } else {
            return new Message(MessageType.PLACE_BID_RESPONSE, "Bid too low or session closed");
        }
    } catch (BidTooLowException e) {
        return new Message(MessageType.PLACE_BID_RESPONSE, "Bid phải cao hơn giá hiện tại: " + e.getMessage());
    } catch (AuctionClosedException e) {
        return new Message(MessageType.PLACE_BID_RESPONSE, "Phiên đấu giá đã đóng");
    } catch (Exception e) {
        return new Message(MessageType.ERROR, "Lỗi server: " + e.getMessage());
    }
}
```

2. **Client**: `ClientProtocolHandler.lastError()` đã xử lý tốt, không cần sửa.

**Phụ thuộc**: Hải phải hoàn thành custom exception (P1-H3) trước.

---

## 🎯 Phase 3 (Ngày 7-9): Nâng Cao + Harden

### ✅ Task P3-N6: Protocol Cho Auto-Bid + Anti-Snipe

**Tạo AutoBidConfig model** (nếu Hải chưa tạo):

**File**: `shared/src/main/java/com/auction/shared/model/auction/AutoBidConfig.java`
```java
package com.auction.shared.model.auction;

import com.auction.shared.model.Entity;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AutoBidConfig extends Entity {
    private int bidderId;
    private int sessionId;
    private double maxBid;
    private double increment;
    private LocalDateTime registeredAt;
    
    public AutoBidConfig(int id, int bidderId, int sessionId, 
                         double maxBid, double increment) {
        super(id);
        this.bidderId = bidderId;
        this.sessionId = sessionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }
}
```

**Test serialize/deserialize**:
- Tạo AutoBidConfig object
- Gửi qua socket
- Verify server nhận đúng

---

### ✅ Task P3-N7: Harden Network

**Công việc**:

1. **Thay System.out/err bằng logging**:
```java
// Thay vì:
System.out.println("Connected to server");
System.err.println("Connection failed");

// Dùng:
private static final Logger logger = Logger.getLogger(SocketClient.class.getName());
logger.info("Connected to server");
logger.warning("Connection failed");
```

2. **Giảm log spam**:
- Chỉ log khi có lỗi hoặc event quan trọng
- Không log mỗi message gửi/nhận (quá nhiều)

3. **Retry chuẩn**:
- `ClientProtocolHandler.send()` đã có retry 1 lần → OK
- Không cần over-engineer reconnect logic

---

## 🎯 Phase 4 (Ngày 10): Polish

### ✅ Task P4-N8: Viết Docs

**File**: `docs/protocol.md`

**Nội dung**:
```markdown
# Protocol Documentation

## Message Structure
- `type`: MessageType enum
- `data`: Object payload
- `errorMessage`: String (nếu có lỗi)
- `success`: boolean
- `timestamp`: String
- `correlationId`: String (RPC only)

## Request/Response Pairs

### Authentication
| Request | Payload | Response | Payload |
|---------|---------|----------|---------|
| LOGIN_REQUEST | String[2] (username, password) | LOGIN_RESPONSE | User object hoặc error |
| REGISTER_REQUEST | String[4] (username, password, email, role) | REGISTER_RESPONSE | "OK" hoặc error |

### Auction Operations
| Request | Payload | Response | Payload |
|---------|---------|----------|---------|
| VIEW_AUCTIONS_REQUEST | null | VIEW_AUCTIONS_RESPONSE | List<AuctionSession> |
| CREATE_AUCTION_REQUEST | AuctionSession | CREATE_AUCTION_RESPONSE | AuctionSession hoặc error |

### Bidding
| Request | Payload | Response | Payload |
|---------|---------|----------|---------|
| PLACE_BID_REQUEST | String[3] (sessionId, bidderId, amount) | PLACE_BID_RESPONSE | "OK" hoặc error |
| GET_BIDS_REQUEST | int (sessionId) | GET_BIDS_RESPONSE | List<Bid> |

### Item Management
| Request | Payload | Response | Payload |
|---------|---------|----------|---------|
| UPDATE_ITEM_REQUEST | Item | UPDATE_ITEM_RESPONSE | "OK" hoặc error |
| DELETE_ITEM_REQUEST | int (itemId) | DELETE_ITEM_RESPONSE | "OK" hoặc error |

### Auto-Bidding
| Request | Payload | Response | Payload |
|---------|---------|----------|---------|
| REGISTER_AUTO_BID_REQUEST | AutoBidConfig | REGISTER_AUTO_BID_RESPONSE | "OK" hoặc error |
| CANCEL_AUTO_BID_REQUEST | int[2] (sessionId, bidderId) | CANCEL_AUTO_BID_RESPONSE | "OK" hoặc error |

### Admin
| Request | Payload | Response | Payload |
|---------|---------|----------|---------|
| BAN_USER_REQUEST | int (userId) | BAN_USER_RESPONSE | "OK" hoặc error |
| GET_ALL_USERS_REQUEST | null | GET_ALL_USERS_RESPONSE | List<User> |

## Server Push (No correlationId)
| Push Type | Payload | Trigger |
|-----------|---------|---------|
| AUCTION_CREATED_PUSH | AuctionSession | Khi có phiên mới được tạo |
| AUCTION_UPDATED_PUSH | AuctionSession | Khi có bid mới |
| CLOSE_AUCTION_PUSH | AuctionSession | Khi phiên tự động đóng |
| AUCTION_EXTENDED_PUSH | AuctionSession | Khi phiên gia hạn (anti-snipe) |

## Error Handling
- `success = false` → check `errorMessage` hoặc `data` (String)
- Client retry 1 lần nếu mất kết nối
- Server catch exception → trả ERROR message
```

---

## 📋 Tóm Tắt Task

| Phase | Task | File | Ước tính | Phụ thuộc |
|-------|------|------|---------|-----------|
| 1 | P1-N1 | MessageType.java | 0.5h | Không |
| 1 | P1-N2 | ServerProtocolHandler.java | 2h | Hải P1-H3, P3-H7 |
| 1 | P1-N3 | ClientProtocolHandler.java | 1.5h | Không |
| 2 | P2-N4 | SocketClient.java | 1h | Không |
| 2 | P2-N5 | Error propagation | 1h | Hải P1-H3 |
| 3 | P3-N6 | AutoBidConfig + test | 2h | Hải P3-H7 |
| 3 | P3-N7 | Logging + cleanup | 2h | Không |
| 4 | P4-N8 | docs/protocol.md | 2h | Tất cả |
| **TỔNG** | **8 task** | | **~12h** | |

---

## ⚠️ Lưu Ý Quan Trọng

> [!IMPORTANT]
> **Phụ thuộc Hải**: P1-N2, P2-N5, P3-N6 cần chờ Hải hoàn thành:
> - P1-H3: Custom exception
> - P3-H7: AutoBidService
> - ItemDAO methods (updateItem, deleteItem)

> [!WARNING]
> **Serialize/Deserialize**: AutoBidConfig phải implement Serializable và test qua socket thực.

> [!TIP]
> **Test từng MessageType**: Sau khi thêm MessageType mới, test ngay bằng cách gửi request thủ công từ client.

---

## ✅ Verification Plan

### Compile
- `mvn compile -pl shared,server,client` → xanh

### Manual Test
- Gửi UPDATE_ITEM_REQUEST → verify server nhận đúng
- Gửi REGISTER_AUTO_BID_REQUEST → verify AutoBidConfig serialize đúng
- Trigger CLOSE_AUCTION_PUSH → verify client nhận push

### Integration Test
- Chạy server + client → test toàn bộ flow mới

---

> **Cập nhật**: 2026-05-12
> **Người tạo**: Duy (QA Lead) — thay mặt Hoàng
> **Trạng thái**: Chờ phê duyệt
