# 📖 Hướng dẫn Codebase cho HOÀNG — Networking Layer

> **Vai trò của Hoàng:** Tầng mạng TCP Socket — giao tiếp Client ↔ Server realtime  
> **Phạm vi chính:** `server/network/` + `client/network/` + `shared/protocol/` + `client/RealtimeAuctionBus.java`

---

## 🗺️ Các file của Hoàng trong hệ thống

```
auction_system/
├── shared/protocol/
│   ├── Message.java          ← "Phong bì" chứa mọi dữ liệu gửi qua mạng
│   └── MessageType.java      ← 40+ loại message (enum)
│
├── server/network/
│   ├── SocketServer.java     ← "Quầy tiếp tân": lắng nghe client kết nối
│   ├── ClientHandler.java    ← "Nhân viên": phục vụ riêng 1 client
│   ├── ClientBroadcastHub.java ← "Loa phóng thanh": gửi cùng lúc cho tất cả
│   └── ServerProtocolHandler.java ← "Tổng đài": route message đến đúng handler
│
└── client/network/
    ├── ClientConnection.java  ← Singleton giữ kết nối tới server
    ├── SocketClient.java      ← Core: gửi/nhận qua TCP, xử lý RPC + Push
    └── ClientProtocolHandler.java ← API đơn giản cho UI controllers dùng
```

---

## 🔌 shared/protocol/Message.java — "Phong bì" của mọi thứ

Mọi thông tin giữa Client và Server đều được đóng gói trong `Message`.

```java
public class Message implements Serializable {
    private MessageType type;      // Loại message (LOGIN_REQUEST, PLACE_BID_REQUEST,...)
    private Object data;           // Dữ liệu đi kèm (User, AuctionSession, String[],...)
    private String errorMessage;   // Thông báo lỗi (nếu thất bại)
    private boolean success;       // true = thành công, false = thất bại
    private String timestamp;      // Thời điểm tạo message
    private String correlationId;  // UUID để ghép cặp request-response
}
```

### Tại sao cần `correlationId`?

Vấn đề: Client gửi 3 request liên tiếp nhanh. Server xử lý và gửi 3 response. Client nhận được 3 response — nhưng response nào của request nào?

Giải pháp: Mỗi request gắn UUID ngẫu nhiên (correlationId). Server copy lại UUID đó vào response. Client dùng UUID để ghép cặp.

```
Client gửi:  LOGIN_REQUEST  [correlationId="abc-123"]
             PLACE_BID_REQUEST [correlationId="xyz-456"]

Server gửi:  LOGIN_RESPONSE  [correlationId="abc-123"] ← Client biết đây là response của LOGIN
             PLACE_BID_RESPONSE [correlationId="xyz-456"]
```

### Hai constructor:
```java
// Khi thành công: data chứa kết quả
new Message(MessageType.LOGIN_RESPONSE, userObject)
// → success=true, data=userObject, errorMessage=null

// Khi thất bại: truyền String error
new Message(MessageType.LOGIN_RESPONSE, "Invalid credentials")
// → success=false, data=null, errorMessage="Invalid credentials"
```

---

## 📋 shared/protocol/MessageType.java — Danh sách 40+ loại message

Chia làm 2 loại:

### Request/Response (RPC — có correlationId):
```
LOGIN_REQUEST ←→ LOGIN_RESPONSE
REGISTER_REQUEST ←→ REGISTER_RESPONSE
VIEW_AUCTIONS_REQUEST ←→ VIEW_AUCTIONS_RESPONSE
CREATE_AUCTION_REQUEST ←→ CREATE_AUCTION_RESPONSE
PLACE_BID_REQUEST ←→ PLACE_BID_RESPONSE
GET_BIDS_REQUEST ←→ GET_BIDS_RESPONSE
UPDATE_ITEM_REQUEST ←→ UPDATE_ITEM_RESPONSE
DELETE_ITEM_REQUEST ←→ DELETE_ITEM_RESPONSE
REGISTER_AUTO_BID_REQUEST ←→ REGISTER_AUTO_BID_RESPONSE
CANCEL_AUTO_BID_REQUEST ←→ CANCEL_AUTO_BID_RESPONSE
BAN_USER_REQUEST ←→ BAN_USER_RESPONSE
GET_ALL_USERS_REQUEST ←→ GET_ALL_USERS_RESPONSE
```

### Server Push (Server chủ động gửi — KHÔNG có correlationId):
```
AUCTION_UPDATED_PUSH    ← Giá vừa thay đổi (ai đó bid)
AUCTION_CREATED_PUSH    ← Phiên mới được tạo
CLOSE_AUCTION_PUSH      ← Phiên vừa đóng (hết giờ)
AUCTION_EXTENDED_PUSH   ← Anti-snipe: phiên gia hạn thêm 60s
```

---

## 🏢 server/network/SocketServer.java — Quầy tiếp tân

```java
public class SocketServer {
    private final int port;                     // Cổng TCP (mặc định 5000)
    private ServerSocket serverSocket;           // Socket lắng nghe kết nối đến
    private final ExecutorService threadPool;    // Pool 10 thread để xử lý clients
    private volatile boolean isRunning;         // volatile: thread-safe flag
}
```

### `start()` — Vòng lặp chính:
```java
public void start() {
    serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress(host, port));
    isRunning = true;
    
    while (isRunning) {
        // 1. Chờ client kết nối (blocking — dừng tại đây cho đến khi có client)
        Socket clientSocket = serverSocket.accept();
        
        // 2. Tạo ClientHandler để phục vụ client này
        // 3. Giao cho thread pool chạy (không chặn vòng lặp chính)
        threadPool.execute(new ClientHandler(clientSocket));
        
        // → Vòng lặp tiếp tục, chờ client tiếp theo
    }
}
```

**Tại sao dùng ThreadPool?**  
Nếu không có pool, mỗi client cần 1 thread mới → tạo thread tốn kém. Pool tái sử dụng 10 thread cho tối đa 10 clients đồng thời.

---

## 👤 server/network/ClientHandler.java — Nhân viên phục vụ 1 client

Mỗi client kết nối → server tạo 1 `ClientHandler` → chạy trên 1 thread từ pool.

```java
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ServerProtocolHandler protocolHandler = new ServerProtocolHandler();
    private final Object writeLock = new Object();  // Bảo vệ khi write
}
```

### `run()` — Vòng đời của 1 handler:
```java
@Override
public void run() {
    // Bước 1: Mở stream để đọc/ghi Java Object
    objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
    objectOutputStream.flush();  // QUAN TRỌNG: flush trước để tránh deadlock
    objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
    
    // Bước 2: Đăng ký vào BroadcastHub để nhận push
    ClientBroadcastHub.register(this);
    
    // Bước 3: Vòng lặp đọc message
    while (true) {
        Message message = (Message) objectInputStream.readObject();  // Blocking
        
        // Bước 4: Xử lý và gửi response
        Message response = protocolHandler.handleMessage(message);
        sendLocked(response);  // Thread-safe write
    }
    
    // Khi client disconnect → EOFException → thoát vòng lặp
    // finally: unregister khỏi BroadcastHub + đóng socket
}
```

### `sendLocked(Message)` — Thread-safe write:
```java
private void sendLocked(Message m) throws IOException {
    synchronized (writeLock) {
        // Chỉ 1 thread vào đây cùng lúc
        objectOutputStream.writeObject(m);
        objectOutputStream.flush();
    }
}
```

**Tại sao cần writeLock?**  
`ClientHandler` có thể bị gọi từ 2 nơi đồng thời:
1. Thread riêng của handler (gửi response sau request)
2. Thread của BroadcastHub (gửi push khi có bid)

Không có lock → 2 thread cùng write → corrupted data trên socket.

### `deliverPush(Message)` — Nhận push từ BroadcastHub:
```java
void deliverPush(Message push) {
    try {
        sendLocked(push);  // Dùng cùng writeLock → an toàn
    } catch (IOException e) {
        System.err.println("Push failed for client: " + clientSocket.getInetAddress());
    }
}
```

---

## 📡 server/network/ClientBroadcastHub.java — Loa phóng thanh (Observer Pattern)

```java
public final class ClientBroadcastHub {
    // Set thread-safe (ConcurrentHashMap.newKeySet() = ConcurrentHashSet)
    private static final Set<ClientHandler> handlers = ConcurrentHashMap.newKeySet();
    
    public static void register(ClientHandler handler)   { handlers.add(handler); }
    public static void unregister(ClientHandler handler) { handlers.remove(handler); }
    
    public static void broadcast(Message push) {
        for (ClientHandler h : handlers) {
            h.deliverPush(push);  // Gửi cùng message đến TẤT CẢ client
        }
    }
}
```

**Observer Pattern:** `ClientBroadcastHub` là Subject, `ClientHandler` là Observer.  
Khi có sự kiện (bid thành công, phiên đóng,...) → `broadcast()` → tất cả observer nhận thông báo.

**Tại sao dùng `ConcurrentHashMap.newKeySet()`?**  
Clients kết nối/ngắt kết nối liên tục từ nhiều thread. Set này thread-safe: nhiều thread có thể add/remove cùng lúc không bị lỗi.

---

## 🎯 server/network/ServerProtocolHandler.java — Tổng đài

```java
public class ServerProtocolHandler {
    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final AutoBidService autoBidService;
    
    public Message handleMessage(Message message) {
        Message response = switch (message.getType()) {
            case LOGIN_REQUEST    -> handleLoginRequest(message.getData());
            case VIEW_AUCTIONS_REQUEST -> handleViewAuctions();
            case PLACE_BID_REQUEST -> handlePlaceBid(message.getData());
            // ... 11 case khác
            default -> new Message(MessageType.ERROR, "Unknown type");
        };
        return tag(message, response);  // Copy correlationId
    }
}
```

### `tag(request, response)`:
```java
private static Message tag(Message request, Message response) {
    if (request.getCorrelationId() != null) {
        response.setCorrelationId(request.getCorrelationId());
    }
    return response;
}
```
→ Đây là cách ghép cặp RPC. Client gửi với correlationId "abc" → Server gửi lại response với cùng "abc".

### Broadcast sau khi xử lý:
```java
private Message handlePlaceBid(Object data) {
    // ... parse data, gọi bidService.placeBid()
    if (success) {
        AuctionSession refreshed = auctionService.getSessionById(sessionId);
        // Broadcast tới TẤT CẢ clients đang kết nối
        ClientBroadcastHub.broadcast(new Message(MessageType.AUCTION_UPDATED_PUSH, refreshed));
        return new Message(MessageType.PLACE_BID_RESPONSE, "Bid placed successfully");
    }
}
```

---

## 🔗 client/network/ClientConnection.java — Singleton kết nối

```java
public final class ClientConnection {
    private static volatile ClientConnection instance;  // volatile cho double-checked locking
    private final Object connectionLock = new Object();
    private SocketClient socketClient;
    
    // Singleton với double-checked locking (thread-safe)
    public static ClientConnection getInstance() {
        if (instance == null) {                // Check lần 1 (không lock, nhanh)
            synchronized (ClientConnection.class) {
                if (instance == null) {         // Check lần 2 (có lock, an toàn)
                    instance = new ClientConnection();
                }
            }
        }
        return instance;
    }
}
```

**Đọc config từ `client.properties`:**
```properties
server.host=localhost
server.port=5000
```

**`connect()`:**
```java
public boolean connect() {
    synchronized (connectionLock) {
        // Nếu đã kết nối rồi → trả về ngay
        if (socketClient != null && socketClient.isConnected()) return true;
        // Tạo SocketClient mới và kết nối
        socketClient = new SocketClient(host, port);
        return socketClient.connect();
    }
}
```

---

## ⚡ client/network/SocketClient.java — Tim của networking client

Đây là file phức tạp nhất. Nó xử lý đồng thời:
- **RPC**: Gửi request, chờ response (synchronous từ góc nhìn caller)
- **Push**: Nhận server push, forward tới UI (asynchronous)

### Constants:
```java
private static final long RPC_TIMEOUT_SEC = 60;    // Đợi tối đa 60s cho response
private static final int CONNECT_TIMEOUT_MS = 5000; // Timeout kết nối 5s
private static final int READ_TIMEOUT_MS = 15000;   // Timeout đọc 15s
```

### State:
```java
private final ConcurrentHashMap<String, CompletableFuture<Message>> pendingRequests;
// Map: correlationId → Future đang chờ response
// Ví dụ: {"abc-123" → Future<Message>, "xyz-456" → Future<Message>}
```

### `connect()`:
```java
public boolean connect() {
    socket = new Socket();
    socket.connect(new InetSocketAddress(host, port), 5000);
    socket.setSoTimeout(15000);    // Đọc timeout 15s
    socket.setKeepAlive(true);     // Giữ kết nối sống
    
    objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    objectOutputStream.flush();    // Flush trước để tránh deadlock
    objectInputStream = new ObjectInputStream(socket.getInputStream());
    
    isConnected = true;
    startReader();  // Bắt đầu background thread đọc
    return true;
}
```

### `startReader()` + `readLoop()` — Thread đọc liên tục:
```java
private void startReader() {
    readerThread = new Thread(this::readLoop, "auction-socket-reader");
    readerThread.setDaemon(true);  // Tự tắt khi app tắt
    readerThread.start();
}

private void readLoop() {
    while (!stopped && isConnected) {
        try {
            Message incoming = (Message) objectInputStream.readObject();  // Blocking 15s
            dispatchIncoming(incoming);  // Phân loại: RPC response hay Push?
        } catch (SocketTimeoutException e) {
            // OK — timeout định kỳ để check !stopped
        }
    }
}
```

### `dispatchIncoming(Message)` — Phân loại message:
```java
private void dispatchIncoming(Message m) {
    String cid = m.getCorrelationId();
    
    // Case 1: Có correlationId → đây là RPC response
    if (cid != null && !cid.trim().isEmpty()) {
        CompletableFuture<Message> fut = pendingRequests.remove(cid);
        if (fut != null) {
            fut.complete(m);  // Giải phóng thread đang chờ ở sendMessage()
            return;
        }
    }
    
    // Case 2: Không có correlationId → đây là server PUSH
    MessageType type = m.getType();
    if (type == AUCTION_UPDATED_PUSH || type == AUCTION_CREATED_PUSH
            || type == CLOSE_AUCTION_PUSH || type == AUCTION_EXTENDED_PUSH) {
        RealtimeAuctionBus.dispatch(m);  // Forward tới UI
    }
}
```

### `sendMessage(Message)` — Gửi request và chờ response:
```java
public Message sendMessage(Message message) {
    // Bước 1: Gán correlationId
    String cid = UUID.randomUUID().toString();
    message.setCorrelationId(cid);
    
    // Bước 2: Tạo Future và đăng ký vào map
    CompletableFuture<Message> fut = new CompletableFuture<>();
    pendingRequests.put(cid, fut);
    
    // Bước 3: Ghi lên socket (thread-safe với writeLock)
    synchronized (writeLock) {
        objectOutputStream.writeObject(message);
        objectOutputStream.flush();
    }
    
    // Bước 4: CHỜ response tối đa 60s
    // Thread này bị block tại đây cho đến khi readLoop() gọi fut.complete()
    Message response = fut.get(60, TimeUnit.SECONDS);
    return response;
}
```

**Luồng hoàn chỉnh:**
```
UI thread gọi sendMessage()
  → Tạo Future, lưu vào map với key=cid
  → Ghi message lên socket
  → fut.get(60s) → BLOCK (ngủ, chờ)

background readLoop() (thread khác):
  → Đọc response từ socket
  → Tìm Future theo correlationId
  → fut.complete(response) → WAKE UP UI thread

UI thread tiếp tục:
  → Nhận response, return về cho caller
```

---

## 🚌 client/RealtimeAuctionBus.java — Observer trên client

```java
public class RealtimeAuctionBus {
    // List thread-safe (CopyOnWriteArrayList: read nhanh, write slow but safe)
    private static final List<Consumer<AuctionSession>> listeners = new CopyOnWriteArrayList<>();
    
    public static void addAuctionListener(Consumer<AuctionSession> listener) {
        listeners.add(listener);
    }
    
    public static void removeAuctionListener(Consumer<AuctionSession> listener) {
        listeners.remove(listener);
    }
    
    // Gọi bởi SocketClient khi nhận push từ server
    public static void dispatch(Message message) {
        Object data = message.getData();
        if (!(data instanceof AuctionSession session)) return;
        
        // Platform.runLater: phải update UI trên JavaFX Application Thread
        Platform.runLater(() -> {
            for (Consumer<AuctionSession> listener : listeners) {
                try {
                    listener.accept(session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
```

**Cách UI đăng ký:**
```java
// Trong BidderDashboardController.initialize():
Consumer<AuctionSession> listener = (updatedSession) -> {
    // Tìm card hiển thị session này và update giá
    updateCardPrice(updatedSession);
};
RealtimeAuctionBus.addAuctionListener(listener);
```

---

## 🔄 Flow Diagrams

### Login:
```
User bấm Login
  → LoginController.handleLogin("alice", "123")
  → ClientProtocolHandler.login("alice", "123")
  → send(LOGIN_REQUEST, ["alice","123"])
    → ClientConnection.connect()  [lần đầu]
    → SocketClient.sendMessage(message)
    → Ghi xuống socket → chờ ở fut.get()
      
      [SERVER]
      ClientHandler đọc message
      → ServerProtocolHandler.handleLoginRequest()
      → UserService.loginUser() → UserDAO.login() → MySQL
      → Nếu OK: tạo User object
      → new Message(LOGIN_RESPONSE, userObject) [correlationId copy lại]
      → sendLocked(response) → gửi qua socket
      
    ← readLoop() nhận response
    ← fut.complete(response) → UI thread thức dậy
  ← login() trả về User object
  → SessionContext.setCurrentUser(user)
  → Navigate tới Dashboard
```

### Place Bid + Realtime Update:
```
Bidder A bấm "Place Bid" 500,000
  → ClientProtocolHandler.placeBid(1, 5, 500000)
  → SocketClient.sendMessage(PLACE_BID_REQUEST)
  → Server: BidService.placeBid(1, 5, 500000)
    → synchronized(lockForSession(1))
    → AuctionSession.updateCurrentPrice(bid)
    → BidDAO.saveBid()
    → AuctionSessionDAO.updateSession()
    → ClientBroadcastHub.broadcast(AUCTION_UPDATED_PUSH, session)
      → ClientHandler_A.deliverPush() → Bidder A nhận push
      → ClientHandler_B.deliverPush() → Bidder B nhận push  
      → ClientHandler_C.deliverPush() → Bidder C nhận push
    → Trả PLACE_BID_RESPONSE về Bidder A

Tất cả client:
  SocketClient.readLoop() nhận AUCTION_UPDATED_PUSH
  → dispatchIncoming(): không có correlationId → gọi RealtimeAuctionBus.dispatch()
  → Platform.runLater() → tất cả listeners được gọi
  → ProductCardController.onAuctionUpdate() → update label giá mới
```

### Anti-Snipe (30s cuối):
```
Bidder bid vào lúc còn 25s
  → BidService.placeBid() → bid thành công
  → checkAndExtendForAntiSnipe(session):
    secondsRemaining = 25 → < 30 → GIA HẠN!
    session.setEndTime(now + 60s)
    ClientBroadcastHub.broadcast(AUCTION_EXTENDED_PUSH, session)

Tất cả client:
  → RealtimeAuctionBus.dispatch()
  → ProductCardController nhận → update endTime → countdown timer tự chạy lại
```

### 4. Anti-sniping cho auto-bid

File chính: `server/src/main/java/com/auction/server/service/AutoBidService.java`

**Vấn đề:**
Trước đây, chỉ manual bid mới trigger anti-sniping. Auto-bid không gọi `checkAndExtendForAntiSnipe()` → nếu auto-bid xảy ra trong 30s cuối, phiên không được gia hạn.

**Giải pháp:**
Thêm anti-sniping logic vào `AutoBidService.processOneAutoBid()`:

```java
// Sau khi auto-bid thành công
session.updateCurrentPrice(autoBid);
bidDAO.saveBid(autoBid, sessionId);

// Thêm anti-sniping check
checkAndExtendForAntiSnipe(session);

auctionSessionDAO.updateSession(session);
```

**Method mới trong `AutoBidService`:**
```java
private static final long SNIPE_WINDOW_SEC = 30;
private static final long EXTENSION_SEC = 60;

private void checkAndExtendForAntiSnipe(AuctionSession session) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = session.getEndTime();
    long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);
    
    if (secondsRemaining >= 0 && secondsRemaining <= SNIPE_WINDOW_SEC) {
        LocalDateTime newEnd = endTime.plusSeconds(EXTENSION_SEC);
        session.setEndTime(newEnd);
        
        // Broadcast extension push
        ClientBroadcastHub.broadcast(
            new Message(MessageType.AUCTION_EXTENDED_PUSH, session)
        );
    }
}
```

**Kết quả:**
- Auto-bid và manual bid đều trigger anti-sniping
- Phiên được gia hạn đồng đều cho cả 2 loại bid

### 5. Dashboard countdown realtime update

File chính:
- `client/src/main/java/com/auction/client/controller/BidderScene/BidderDashboardController.java`
- `client/src/main/java/com/auction/client/controller/Card/ProductCardController.java`

**Vấn đề:**
Khi server broadcast `AUCTION_EXTENDED_PUSH` sau anti-sniping, dashboard không cập nhật `endTime` của card đang hiển thị → countdown timer vẫn chạy theo giờ cũ → hiển thị "ENDED" sai.

**Giải pháp:**
Dashboard realtime listener cập nhật card đang hiển thị trước khi reload list:

```java
// Trong BidderDashboardController.setupRealtimeListener()
realtimeListener = updatedSession -> {
    // Push mang theo endTime/currentPrice mới
    AuctionCache.addOrReplace(updatedSession);
    
    // Cập nhật card đang hiển thị ngay
    ProductCardController visibleCard = cardControllers.get(updatedSession.getId());
    if (visibleCard != null) {
        visibleCard.setAuctionSession(updatedSession);
    }
    
    // Fetch lại active từ server ở nền
    loadActiveAuctionsAsync();
};
```

**Trong `ProductCardController.setAuctionSession()`:**
```java
public void setAuctionSession(AuctionSession session) {
    // Stop countdown cũ trước khi rebind
    if (countdownTimer != null) {
        countdownTimer.stop();
    }
    this.session = session;
    // ... bind data mới và start countdown mới
}
```

**Kết quả:**
- Khi anti-sniping trigger, tất cả dashboard nhận push
- Card countdown tự động restart với `endTime` mới
- Không còn hiển thị "ENDED" sai khi phiên vừa được gia hạn

---

## 🔄 Cập nhật source mới nhất Hoàng cần biết

### 1. Client RPC dùng lock chung để ổn định socket

File chính: `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`

Do app dùng `ClientConnection` singleton, nhiều controller gọi request cùng lúc có thể làm socket bị đóng giữa chừng.
Hiện tại mọi RPC được serialize bằng:

```java
private static final Object RPC_LOCK = new Object();
```

Ý nghĩa:

- tránh lỗi `Socket write failed: Socket closed`;
- controller A không disconnect/reconnect socket khi controller B đang gửi request;
- phù hợp cho demo ổn định hơn, dù request không còn chạy song song hoàn toàn.

### 2. `SocketClient.isConnected()` kiểm tra socket thật

File chính: `client/src/main/java/com/auction/client/network/SocketClient.java`

`isConnected()` không chỉ dựa vào boolean nữa, mà kiểm tra cả socket:

```java
return isConnected && socket != null && socket.isConnected() && !socket.isClosed();
```

Mục đích: tránh trường hợp flag còn `true` nhưng socket thực tế đã đóng.

### 3. README và build script ưu tiên fat JAR

Theo yêu cầu nộp bài, README hiện hướng dẫn chạy bằng:

```bash
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Script `build-fat-jar.bat` đã đổi sang dùng `mvn` từ `PATH`, không hardcode đường dẫn máy cá nhân.

---

## ❓ FAQ cho Hoàng

**Q: Tại sao flush() sau khi tạo ObjectOutputStream?**  
A: Java ObjectOutputStream gửi một "stream header" khi được tạo. Nếu không flush ngay, đầu kia (ObjectInputStream) sẽ block mãi chờ header này. Flush đảm bảo header được gửi đi ngay.

**Q: `volatile boolean isRunning/isConnected` là gì?**  
A: Khi nhiều thread cùng đọc/ghi biến, CPU có thể cache giá trị cũ. `volatile` buộc mọi thread đọc giá trị thực từ RAM, không dùng cache.

**Q: Tại sao `readLoop()` bắt `SocketTimeoutException` nhưng không làm gì?**  
A: Socket có `setSoTimeout(15000)` — sau 15s không có dữ liệu thì throw `SocketTimeoutException`. Catch và tiếp tục vòng lặp để định kỳ kiểm tra `!stopped`. Nếu không có timeout, `readObject()` block vĩnh viễn → không bao giờ biết khi nào app muốn tắt.

**Q: Tại sao BroadcastHub dùng `ConcurrentHashMap.newKeySet()` thay vì `HashSet`?**  
A: Nhiều thread add/remove handler cùng lúc (`broadcast()` chạy trên thread của caller, `register/unregister` chạy trên thread của ClientHandler). HashSet không thread-safe → ConcurrentModificationException.

**Q: Tại sao có `objectOutputStream.flush()` sau mỗi `writeObject()`?**  
A: Java có buffer khi ghi. Không flush → dữ liệu nằm trong buffer, chưa gửi → đầu kia không nhận được. Flush đẩy buffer xuống socket ngay lập tức.

**Q: Điều gì xảy ra nếu server crash?**  
A: `readLoop()` catch `EOFException` hoặc `IOException` → `handleConnectionLoss()`:
1. Đánh dấu `isConnected = false`
2. Fail tất cả pending futures (các request đang chờ response)
3. Đóng socket
→ `ClientProtocolHandler.send()` sẽ retry 1 lần với kết nối mới.
