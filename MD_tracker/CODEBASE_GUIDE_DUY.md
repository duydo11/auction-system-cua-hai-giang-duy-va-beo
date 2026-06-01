# 📖 Hướng dẫn Codebase cho DUY — Concurrency, Testing, CI/CD

> **Vai trò của Duy:** Đảm bảo hệ thống chạy đúng khi nhiều người dùng đồng thời, viết test, CI/CD  
> **Phạm vi chính:** `server/service/BidService.java`, `server/service/AutoBidService.java`, `server/service/AuctionScheduler.java`, `server/src/test/`, `.github/workflows/`

---

## 🗺️ Các file của Duy

```
server/
├── service/
│   ├── BidService.java         ← ★ Đặt giá + concurrency lock + anti-snipe
│   ├── AutoBidService.java     ← ★ Auto-bid engine với PriorityQueue
│   ├── AuctionScheduler.java   ← ★ Tự đóng phiên hết giờ (chạy mỗi 10s)
│   ├── AuctionService.java     ← CRUD phiên (Hải viết, Duy test)
│   └── UserService.java        ← CRUD user (Hải viết, Duy test)
│
├── dao/
│   ├── DatabaseConnection.java ← Kết nối MySQL / H2
│   ├── UserDAO.java            ← CRUD user → MySQL
│   ├── AuctionSessionDAO.java  ← CRUD phiên → MySQL
│   ├── BidDAO.java             ← CRUD bid → MySQL
│   └── ItemDAO.java            ← CRUD item → MySQL
│
├── ServiceRegistry.java        ← Singleton registry cho 4 service
│
└── src/test/
    ├── java/com/auction/server/
    │   ├── service/
    │   │   ├── BidServiceTest.java       ← 8 tests
    │   │   ├── AutoBidServiceTest.java   ← 8 tests
    │   │   ├── AntiSnipingTest.java      ← 4 tests
    │   │   ├── UserServiceTest.java      ← 12 tests (nếu có)
    │   │   └── ExceptionHandlingTest.java← 8 tests
    │   ├── dao/
    │   │   ├── DAOTestBase.java          ← Base class cho DAO tests
    │   │   ├── UserDAOTest.java
    │   │   └── ItemDAOTest.java
    │   └── ConcurrencyStressTest.java    ← 50 thread, 500 bid
    └── resources/
        ├── schema-h2.sql                 ← Schema cho H2 test DB
        └── application-test.properties   ← Config test
```

---

## ⚡ BidService.java — Cốt lõi concurrency

### Vấn đề: Race Condition khi 2 người bid cùng lúc

```
Thread A: Đọc currentPrice = 100,000        ← giá cũ
Thread B: Đọc currentPrice = 100,000        ← giá cũ (cùng lúc!)
Thread A: Bid 150,000 > 100,000 → OK → currentPrice = 150,000
Thread B: Bid 120,000 > 100,000 → OK → currentPrice = 120,000 ← SAI! Giá giảm!!
```

### Giải pháp: Lock per-session

```java
public class BidService {
    // Map: sessionId → lock object riêng
    private static final ConcurrentHashMap<Integer, Object> SESSION_BID_LOCKS = new ConcurrentHashMap<>();
    
    private static final long SNIPE_WINDOW_SEC = 30;  // Anti-snipe: 30s cuối
    private static final long EXTENSION_SEC = 60;     // Gia hạn 60s
    
    // Lấy hoặc tạo lock cho session
    private static Object lockForSession(int sessionId) {
        return SESSION_BID_LOCKS.computeIfAbsent(sessionId, id -> new Object());
        // computeIfAbsent: nếu chưa có → tạo new Object() → trả về
        // nếu đã có → trả về cái cũ (thread-safe)
    }
}
```

**Tại sao lock per-session thay vì 1 lock toàn hệ thống?**

```
Lock toàn hệ thống:
  User A bid phiên #1 → LOCK → xử lý 50ms
  User B bid phiên #2 → PHẢI CHỜ user A ← Vô lý! Phiên khác nhau mà!
  
Lock per-session:
  User A bid phiên #1 → LOCK(session#1) → xử lý 50ms
  User B bid phiên #2 → LOCK(session#2) → xử lý song song ← OK!
  User C bid phiên #1 → LOCK(session#1) → PHẢI CHỜ user A ← Đúng! Cùng phiên
```

### `placeBid()` — Logic đầy đủ (cập nhật theo code thực tế):

```java
public boolean placeBid(int sessionId, int bidderId, double amount) {
    synchronized (lockForSession(sessionId)) {
        // ==== Chỉ 1 thread vào đây cùng lúc (cho cùng sessionId) ====
        
        // 1. Load session và bidder từ DB (mới nhất)
        AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
        User bidder = userDAO.getUserById(bidderId);
        
        // 2. Kiểm tra cả 2 cùng lúc — nếu thiếu 1 trong 2 → reject ngay
        if (session == null || bidder == null) {
            return false;
        }
        
        // 3. Ghi nhớ số lượng bid TRƯỚC khi thử
        // Dùng để kiểm tra xem updateCurrentPrice() có chấp nhận bid không
        int bidsBefore = session.getBids().size();
        
        // 4. Tạo Bid object với ID mới từ DB
        int bidId = bidDAO.allocateNextBidId();
        Bid bid = new Bid(bidId, bidder, session, amount);
        
        // 5. Thử cập nhật giá (validate bên trong AuctionSession.updateCurrentPrice)
        session.updateCurrentPrice(bid);
        // updateCurrentPrice() kiểm tra:
        //   - session.isActive() (thời gian + status)
        //   - amount > currentPrice
        //   - bid time trong phạm vi start-end
        // Nếu hợp lệ → thêm bid vào session.bids list
        // Nếu không hợp lệ → KHÔNG thêm (size không đổi)
        
        // 6. Kiểm tra xem bid có được chấp nhận không
        // ⚠️ CHÚ Ý: Logic này check <= (không tăng) → reject
        // Nếu size KHÔNG tăng → bid bị reject → return false ngay
        if (session.getBids().size() <= bidsBefore) {
            return false;
        }
        
        // 7. Bid được chấp nhận → lưu vào DB
        bidDAO.saveBid(bid, sessionId);
        
        // 8. Anti-sniping: kiểm tra nếu bid trong 30s cuối → gia hạn +60s
        checkAndExtendForAntiSnipe(session);
        
        // 9. Cập nhật session vào DB (currentPrice, winner, endTime nếu có anti-snipe)
        auctionSessionDAO.updateSession(session);

        // 10. Kích hoạt auto-bids sau bid thủ công
        // AutoBidService sẽ kiểm tra tất cả auto-bid configs cho session này
        // Nếu có bidder khác đang auto-bid → tự động đặt bid tiếp
        // ⚠️ processAutoBids() có thể tạo thêm bid mới → trigger cascade
        com.auction.server.ServiceRegistry.AUTO_BID_SERVICE.processAutoBids(sessionId, amount);

        // 11. Refresh session từ DB để lấy state mới nhất
        // Vì processAutoBids() có thể đã thay đổi currentPrice/winner
        // Cần refresh để broadcast đúng data
        AuctionSession refreshed = auctionSessionDAO.getSessionById(sessionId);
        
        // 12. Broadcast push tới TẤT CẢ clients đang kết nối
        // AUCTION_UPDATED_PUSH → RealtimeAuctionBus → UI update giá/winner/countdown
        // Dùng refreshed session (nếu có) để đảm bảo data mới nhất
        ClientBroadcastHub.broadcast(new Message(
                MessageType.AUCTION_UPDATED_PUSH,
                refreshed != null ? refreshed : session
        ));

        return true;
    }
}
```

**Giải thích chi tiết các bước:**

**Bước 1-2:** Load data từ DB và validate
- `AuctionSessionDAO.getSessionById()` → trả về `AuctionSession` đầy đủ (seller, item, winner, bids)
- `UserDAO.getUserById()` → trả về `User` (có thể là Bidder/Seller/Admin)
- Check cả 2 cùng lúc để code gọn hơn

**Bước 3:** Ghi nhớ size trước
- `bidsBefore` = số bid hiện tại
- Dùng để detect xem `updateCurrentPrice()` có chấp nhận bid không

**Bước 5:** `session.updateCurrentPrice(bid)` — Logic validation
- File: `shared/model/auction/AuctionSession.java`
- Method này kiểm tra:
  - `isActive()` → thời gian + status (OPEN/RUNNING)
  - `amount > currentPrice` → giá phải cao hơn
  - `bid.getTime()` trong phạm vi `[startTime, endTime]`
- Nếu hợp lệ → `bids.add(bid)` + update `currentPrice` + update `winner`
- Nếu không hợp lệ → KHÔNG thêm vào list (in ra "Invalid bid" hoặc "Time runs out")

**Bước 6:** Check kết quả
- ⚠️ **Quan trọng:** Logic check `<= bidsBefore` (không tăng)
- Nếu size không tăng → bid bị reject → return false
- Nếu size tăng → bid được chấp nhận → tiếp tục

**Bước 10:** Auto-bid cascade
- `ServiceRegistry.AUTO_BID_SERVICE` → singleton instance
- `processAutoBids(sessionId, amount)` → kiểm tra tất cả auto-bid configs
- Nếu có bidder khác đang auto-bid VÀ chưa vượt maxBid → tự động đặt bid
- ⚠️ Auto-bid có thể trigger thêm auto-bid khác → cascade (có break để tránh infinite loop)

**Bước 11:** Refresh session
- Vì `processAutoBids()` có thể đã thay đổi session (giá mới, winner mới)
- Cần load lại từ DB để broadcast đúng state mới nhất
- Nếu refresh fail (null) → dùng session cũ làm fallback

**Bước 12:** Broadcast realtime
- `ClientBroadcastHub.broadcast()` → gửi tới TẤT CẢ `ClientHandler` đang kết nối
- File: `server/network/ClientBroadcastHub.java`
- `AUCTION_UPDATED_PUSH` → client nhận qua `SocketClient.dispatchIncoming()`
- → `RealtimeAuctionBus.dispatch()` → tất cả listeners (dashboard, detail page) update UI


### Anti-Sniping — Chống "lẻn vào phút cuối":

**Vấn đề:** Bidder chờ 29 giây cuối mới bid → không ai kịp phản ứng → thắng dễ dàng.

**Giải pháp:** Nếu bid trong 30s cuối → gia hạn thêm 60s → cho người khác kịp phản ứng.

```java
private void checkAndExtendForAntiSnipe(AuctionSession session) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = session.getEndTime();
    
    long secondsRemaining = ChronoUnit.SECONDS.between(now, endTime);
    
    // Nếu còn 0-30 giây → gia hạn
    if (secondsRemaining >= 0 && secondsRemaining <= SNIPE_WINDOW_SEC) {
        LocalDateTime newEnd = endTime.plusSeconds(EXTENSION_SEC);
        session.setEndTime(newEnd);
        
        // Thông báo TẤT CẢ clients: "phiên vừa gia hạn!"
        ClientBroadcastHub.broadcast(
            new Message(MessageType.AUCTION_EXTENDED_PUSH, session)
        );
        
        System.out.println("Anti-snipe: session #" + session.getId() 
            + " extended to " + newEnd);
    }
}
```

**Ví dụ timeline:**
```
00:00  Phiên bắt đầu (endTime = 10:00)
09:35  Bidder A bid 500,000 → còn 25s < 30s → GIA HẠN → endTime = 11:00
10:45  Bidder B bid 600,000 → còn 15s < 30s → GIA HẠN → endTime = 12:00
11:45  Bidder A bid 700,000 → còn 15s < 30s → GIA HẠN → endTime = 13:00
12:30  30s trôi qua, không ai bid → phiên kết thúc tại 13:00
```

---

## 🤖 AutoBidService.java — Engine tự động đặt giá

### Cấu trúc dữ liệu:

```java
// Map: sessionId → PriorityBlockingQueue (sắp xếp theo thời gian đăng ký)
private final Map<Integer, PriorityBlockingQueue<AutoBidConfig>> autoBidsBySession;

// Mỗi AutoBidConfig chứa:
// - bidderId: ai đăng ký
// - sessionId: phiên nào
// - maxBid: giá tối đa sẵn sàng trả
// - increment: mức tăng mỗi lần
// - registeredAt: thời điểm đăng ký (dùng để ưu tiên FIFO)
```

### `registerAutoBid(config)`:
```java
public boolean registerAutoBid(AutoBidConfig config) {
    int sessionId = config.getSessionId();
    
    // computeIfAbsent: nếu chưa có queue cho session → tạo mới
    autoBidsBySession.computeIfAbsent(sessionId, k -> 
        new PriorityBlockingQueue<>(11, Comparator.comparing(AutoBidConfig::getRegisteredAt))
    );
    
    // Thêm config vào queue (queue tự sắp xếp theo registeredAt)
    autoBidsBySession.get(sessionId).add(config);
    return true;
}
```

### `processAutoBids(sessionId, newBidAmount)` — Logic chính:

Gọi bởi `BidService.placeBid()` sau khi bid thủ công thành công.

```java
public boolean processAutoBids(int sessionId, double newBidAmount) {
    // 1. Lấy queue auto-bid configs cho session này
    PriorityBlockingQueue<AutoBidConfig> queue = autoBidsBySession.get(sessionId);
    if (queue == null || queue.isEmpty()) {
        return false;  // Không có auto-bid nào đăng ký
    }
    
    // 2. Load session từ DB và check active
    AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
    if (session == null || !session.isActive()) {
        return false;  // Session không tồn tại hoặc đã đóng
    }
    
    // 3. Biến flag để track xem có auto-bid nào được kích hoạt không
    boolean anyAutoBidTriggered = false;
    
    // 4. List để lưu configs cần đưa lại vào queue
    List<AutoBidConfig> toRequeue = new ArrayList<>();
    
    // 5. Process từng config theo priority (FIFO theo registeredAt)
    while (!queue.isEmpty()) {
        AutoBidConfig config = queue.poll();  // Lấy config có priority cao nhất
        
        // 5a. Null check (defensive programming)
        if (config == null) break;
        
        // 5b. Bỏ qua nếu auto-bidder đang thắng (không tự bid lại chính mình)
        if (session.getWinner() != null && session.getWinner().getId() == config.getBidderId()) {
            toRequeue.add(config);  // Giữ lại config cho lần sau
            continue;
        }
        
        // 5c. Tính giá auto-bid tiếp theo
        double currentPrice = session.getCurrentPrice();
        double nextBid = Math.min(currentPrice + config.getIncrement(), config.getMaxBid());
        // nextBid = min(giá hiện tại + increment, maxBid)
        // → Đảm bảo không vượt maxBid
        
        // 5d. Kiểm tra xem có thể outbid không
        if (nextBid > currentPrice && nextBid <= config.getMaxBid()) {
            // 6. Tạo auto-bid
            Bid autoBid = new Bid(
                bidDAO.allocateNextBidId(),
                userDAO.getUserById(config.getBidderId()),
                session,
                nextBid
            );
            autoBid.setTime(LocalDateTime.now());  // Set thời gian bid
            
            // 7. Cập nhật giá (⚠️ CHÚ Ý: không check xem updateCurrentPrice có chấp nhận không)
            // Trong thực tế, code này giả định auto-bid luôn hợp lệ vì đã check ở bước 5d
            session.updateCurrentPrice(autoBid);
            
            // 8. Lưu bid vào DB
            bidDAO.saveBid(autoBid, sessionId);
            
            // 9. Cập nhật session vào DB
            auctionSessionDAO.updateSession(session);
            
            // 10. Broadcast update tới tất cả clients
            // ⚠️ CHÚ Ý: Không refresh từ DB như BidService.placeBid()
            // Dùng session object hiện tại để broadcast
            ClientBroadcastHub.broadcast(new Message(MessageType.AUCTION_UPDATED_PUSH, session));
            
            // 11. Log
            logger.info("Auto-bid triggered: bidderId=" + config.getBidderId() +
                    ", amount=" + nextBid);
            
            // 12. Set flag
            anyAutoBidTriggered = true;
            
            // 13. Giữ lại config cho lần sau (bidder có thể bid tiếp nếu bị outbid)
            toRequeue.add(config);
            
            // 14. BREAK sau 1 auto-bid để tránh infinite loop
            // Nếu không break: Auto-bid A → Auto-bid B → Auto-bid A → ... → vô hạn
            // Break → lần bid tiếp theo sẽ trigger processAutoBids lại → an toàn
            break;
        } else {
            // Không thể outbid (đã đạt maxBid hoặc giá không hợp lệ)
            // Giữ lại config cho lần sau
            toRequeue.add(config);
        }
    }
    
    // 15. Đưa tất cả configs lại vào queue
    for (AutoBidConfig config : toRequeue) {
        queue.add(config);
    }
    
    // 16. Return flag
    return anyAutoBidTriggered;
}
```

**Giải thích chi tiết:**

**Bước 3:** Khai báo `anyAutoBidTriggered`
- ⚠️ **Quan trọng:** Phải khai báo biến này, nếu không code sẽ compile error
- Dùng để return về cho caller biết có auto-bid nào được kích hoạt không

**Bước 5:** Process configs theo priority
- `queue.poll()` lấy config có `registeredAt` sớm nhất (FIFO)
- Null check để tránh NPE nếu queue bị modify từ thread khác

**Bước 5b:** Skip nếu đang thắng
- Tránh auto-bidder tự bid lại chính mình
- Ví dụ: A đang thắng với 100k, A auto-bid không nên bid lại 110k

**Bước 5c:** Tính nextBid
- `Math.min(currentPrice + increment, maxBid)` → không vượt maxBid
- Ví dụ: currentPrice=90k, increment=20k, maxBid=100k → nextBid=100k (không phải 110k)

**Bước 7:** ⚠️ Không check kết quả `updateCurrentPrice`
- Code thực tế giả định auto-bid luôn hợp lệ vì đã check ở bước 5d
- Khác với `BidService.placeBid()` có check `session.getBids().size()`
- Đây có thể là edge case bug nếu session bị đóng giữa chừng, nhưng đó là reality

**Bước 10:** ⚠️ Không refresh session từ DB
- Khác với `BidService.placeBid()` có refresh
- Dùng session object hiện tại để broadcast
- Có thể gây lệch data nếu có concurrent updates, nhưng đó là reality

**Bước 14:** Break để tránh infinite loop
- Chỉ process 1 auto-bid mỗi lần gọi
- Lần bid tiếp theo (từ auto-bid này) sẽ trigger `processAutoBids` lại
- → Cascade được kiểm soát từng bước

**Tại sao `break` sau 1 auto-bid?**  
Nếu không break: Auto-bid A bid → trigger Auto-bid B → trigger Auto-bid A → ... → infinite loop. Break sau 1 lần → lần bid tiếp theo sẽ trigger processAutoBids lại → an toàn.


---

## ⏰ AuctionScheduler.java — Tự đóng phiên hết giờ

```java
public class AuctionScheduler {
    private static final int SCAN_INTERVAL_SECONDS = 10;  // Quét mỗi 10s
    
    private final ScheduledExecutorService scheduler;
    
    // Singleton (double-checked locking)
    public static AuctionScheduler getInstance() { ... }
    
    public void start() {
        scheduler.scheduleAtFixedRate(
            this::scanAndCloseExpired,   // Task
            10,                          // Delay lần đầu: 10s
            10,                          // Lặp mỗi: 10s
            TimeUnit.SECONDS
        );
    }
}
```

### `scanAndCloseExpired()`:
```java
private void scanAndCloseExpired() {
    List<AuctionSession> activeSessions = auctionSessionDAO.findAllActiveSessions();
    LocalDateTime now = LocalDateTime.now();
    
    for (AuctionSession session : activeSessions) {
        // Nếu hết giờ VÀ chưa FINISHED
        if (now.isAfter(session.getEndTime()) 
            && (status == OPEN || status == RUNNING)) {
            
            session.setStatus(AuctionStatus.FINISHED);
            auctionSessionDAO.updateSession(session);
            
            // Broadcast push: "phiên #X đã đóng!"
            ClientBroadcastHub.broadcast(
                new Message(MessageType.CLOSE_AUCTION_PUSH, session)
            );
        }
    }
}
```

**Tại sao dùng daemon thread?**  
`t.setDaemon(true)` → thread tự tắt khi JVM exit. Không cần phải gọi `stop()` thủ công khi tắt server.

---

## 📦 ServiceRegistry.java — Singleton Registry

```java
public class ServiceRegistry {
    // 4 service singleton - khởi tạo 1 lần, dùng xuyên suốt
    public static final UserService USER_SERVICE = new UserService();
    public static final AuctionService AUCTION_SERVICE = new AuctionService();
    public static final BidService BID_SERVICE = new BidService();
    public static final AutoBidService AUTO_BID_SERVICE = new AutoBidService();
}
```

`ServerProtocolHandler` lấy service từ đây:
```java
public ServerProtocolHandler() {
    this(ServiceRegistry.USER_SERVICE, ServiceRegistry.AUCTION_SERVICE, 
         ServiceRegistry.BID_SERVICE, ServiceRegistry.AUTO_BID_SERVICE);
}
```

---

## 🗄️ DAO Layer — Kết nối Database

### DatabaseConnection.java:
```java
// Đọc config từ server.properties:
// db.url=jdbc:mysql://localhost:3306/auction_system
// db.user=root
// db.password=

// Hoặc dùng H2 in-memory cho test:
// db.url=jdbc:h2:mem:test
```

### UserDAO — Chi tiết CRUD:

| Method | SQL | Mô tả |
|--------|-----|-------|
| `login(username, password)` | `SELECT id FROM users WHERE username=? AND password=?` | Login → getUserById() |
| `saveUser(user)` | `INSERT INTO users` + INSERT INTO bidders/sellers/admins | Transaction (autoCommit=false) |
| `getUserById(id)` | Thử getBidderById → getSellerById → getAdminById | Tự nhận diện role |
| `updateUser(user)` | `UPDATE users` + UPDATE bảng role | Transaction |
| `deleteUser(id)` | `DELETE FROM bidders/sellers/admins` + `DELETE FROM users` | Xóa con trước, cha sau |
| `getAllUsers()` | `SELECT id FROM users` → getUserById cho mỗi id | List |

### AuctionSessionDAO:

| Method | Mô tả |
|--------|-------|
| `saveSession(session)` | INSERT vào auction_sessions |
| `getSessionById(id)` | SELECT + JOIN lấy seller, item, winner, bids → trả AuctionSession đầy đủ |
| `findAllActiveSessions()` | `WHERE start_time < NOW AND end_time > NOW` |
| `updateSession(session)` | `UPDATE current_price, winner_id WHERE id=?` |

### BidDAO:

| Method | Mô tả |
|--------|-------|
| `saveBid(bid, sessionId)` | INSERT INTO bids |
| `getBidsBySessionId(sessionId, session)` | SELECT bids + JOIN users → List<Bid> |
| `allocateNextBidId()` | `SELECT MAX(id)+1` |

---

## 🧪 Unit Tests

### Chạy tất cả tests:
```bash
cd server
mvn test
# Kết quả: 41 tests, 100% pass
```

### BidServiceTest.java — 8 tests:

```java
// Setup: Mock AuctionSessionDAO, BidDAO, UserDAO bằng Mockito
@BeforeEach
void setUp() {
    mockSessionDAO = mock(AuctionSessionDAO.class);
    mockBidDAO = mock(BidDAO.class);
    mockUserDAO = mock(UserDAO.class);
    bidService = new BidService(mockSessionDAO, mockBidDAO, mockUserDAO);
}

@Test
void testPlaceBid_success() {
    // Given: session active, bidder exists, amount > currentPrice
    when(mockSessionDAO.getSessionById(1)).thenReturn(activeSession);
    when(mockUserDAO.getUserById(5)).thenReturn(bidder);
    
    // When
    boolean result = bidService.placeBid(1, 5, 200.0);
    
    // Then
    assertTrue(result);
    verify(mockBidDAO).saveBid(any(), eq(1));  // Verify bid được lưu
}

@Test
void testPlaceBid_sessionNotFound() {
    when(mockSessionDAO.getSessionById(999)).thenReturn(null);
    assertFalse(bidService.placeBid(999, 5, 200.0));
}

@Test
void testPlaceBid_bidTooLow() {
    // currentPrice = 1000, bid 500 → reject
    activeSession.setCurrentPrice(1000);
    boolean result = bidService.placeBid(1, 5, 500.0);
    assertFalse(result);
}
```

### ConcurrencyStressTest.java — Stress test:

```java
@Test
void testStress_50Threads_5Sessions_10BidsEach() {
    // 50 threads, mỗi thread gửi 10 bid → tổng 500 bid vào 5 session
    ExecutorService pool = Executors.newFixedThreadPool(50);
    AtomicInteger successCount = new AtomicInteger(0);
    
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
        int sessionId = i % 5 + 1;  // Session 1-5
        futures.add(pool.submit(() -> {
            for (int j = 0; j < 10; j++) {
                if (bidService.placeBid(sessionId, ...)) {
                    successCount.incrementAndGet();
                }
            }
        }));
    }
    
    // Đợi tất cả hoàn thành
    for (Future<?> f : futures) f.get();
    
    // Assert: ít nhất 50% bid phải thành công
    assertTrue(successCount.get() >= 250, 
        "Ít nhất 50% bid phải thành công, nhưng chỉ có " + successCount.get() + "/500");
}
```

### AntiSnipingTest.java — 4 tests:

```java
@Test
void testAntiSnipe_bidIn30sWindow_extendsEndTime() {
    // Phiên kết thúc trong 25s nữa
    session.setEndTime(LocalDateTime.now().plusSeconds(25));
    
    bidService.placeBid(1, 5, 200.0);
    
    // endTime phải được gia hạn thêm 60s
    assertTrue(session.getEndTime().isAfter(LocalDateTime.now().plusSeconds(50)));
}

@Test
void testAntiSnipe_bidOutsideWindow_noExtension() {
    // Phiên còn 5 phút → không gia hạn
    session.setEndTime(LocalDateTime.now().plusMinutes(5));
    LocalDateTime originalEnd = session.getEndTime();
    
    bidService.placeBid(1, 5, 200.0);
    
    assertEquals(originalEnd, session.getEndTime());  // Không đổi
}
```

---

## 🔄 CI/CD — GitHub Actions

File: `.github/workflows/maven.yml`

```yaml
name: Java CI with Maven
on: [push, pull_request]

jobs:
  build:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
    runs-on: ${{ matrix.os }}
    
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
    - run: mvn -B test --file pom.xml
```

**Matrix strategy:** Chạy test trên 3 OS → bắt bug liên quan đến đường dẫn file, line ending, encoding.

---

## 🔄 Flow: 2 users bid đồng thời

```
User A (Thread A)                    User B (Thread B)
bid session #1, 150,000             bid session #1, 120,000
     |                                    |
synchronized(lock_session_1)          synchronized(lock_session_1)
     ↓                                    ↓
[ENTER - Thread A vào trước]          [WAIT - Thread B chờ]
  Load session: price=100,000              |
  Bid 150,000 > 100,000 → OK              |
  price = 150,000                          |
  saveBid()                                |
  updateSession()                          |
[EXIT lock]                                |
     |                               [ENTER - Thread B vào]
     |                                  Load session: price=150,000 ← NEWEST
     |                                  Bid 120,000 < 150,000 → REJECT ← ĐÚNG!
     |                               [EXIT lock]
```

### 5. Settlement idempotency — Chặn transaction lặp

File chính: `server/src/main/java/com/auction/server/service/AuctionService.java`

**Vấn đề phát hiện:**
Khi nhiều thread/scheduler cùng gọi `settleAuctionIfExpired()` cho cùng một session, có thể tạo ra nhiều transaction trùng nhau:
- Bidder bị trừ tiền 3 lần
- Seller nhận tiền 3 lần
- Wallet history hiển thị 3 dòng giống nhau

**Nguyên nhân:**
Lock trong memory (`SETTLEMENT_LOCKS`) chỉ chặn được trong cùng JVM instance. Nếu có nhiều request đồng thời hoặc scheduler chạy trước khi status được cập nhật, vẫn có thể bypass lock.

**Giải pháp:**
Thêm idempotency check ở tầng DAO bằng cách:
1. Dùng transaction description có format: `#<sessionId> <itemName>`
2. Trước khi trừ/cộng tiền, check xem transaction với description này đã tồn tại chưa
3. Nếu đã có → skip wallet update

**Code mới trong `AuctionService.settleAuctionIfExpired()`:**
```java
String settlementDescription = "#" + latestSession.getId() + " " + itemName;

// Bidder payment
if (latestWinner != null) {
    boolean alreadyPaid = userDAO.hasTransaction(
        latestWinner.getId(), "BID_PAYMENT", settlementDescription
    );
    if (!alreadyPaid) {
        latestWinner.setAccountBalance(latestWinner.getAccountBalance() - price);
        userDAO.updateUser(latestWinner);
        userDAO.saveTransaction(new Transaction(
            0, latestWinner.getId(), price, "BID_PAYMENT", settlementDescription, paidAt
        ));
    }
}

// Seller income
if (latestSeller != null) {
    boolean alreadyReceived = userDAO.hasTransaction(
        latestSeller.getId(), "AUCTION_SALE", settlementDescription
    );
    if (!alreadyReceived) {
        latestSeller.setAccountBalance(latestSeller.getAccountBalance() + price);
        userDAO.updateUser(latestSeller);
        userDAO.saveTransaction(new Transaction(
            0, latestSeller.getId(), price, "AUCTION_SALE", settlementDescription, paidAt
        ));
    }
}
```

**Code mới trong `UserDAO`:**
```java
public boolean hasTransaction(int userId, String type, String description) {
    String sql = "SELECT 1 FROM transactions WHERE user_id = ? AND type = ? AND description = ? LIMIT 1";
    // ... check existence
}
```

### 6. Wallet UI deduplication — Lọc transaction trùng

File chính:
- `client/src/main/java/com/auction/client/controller/BidderScene/Wallet1Controller.java`
- `client/src/main/java/com/auction/client/controller/SellerScene/Wallet2Controller.java`

**Vấn đề:**
Dữ liệu cũ trong DB đã có transaction bị lặp 3 lần. Dù settlement mới đã idempotent, UI vẫn hiển thị 3 dòng lịch sử giống nhau cho data cũ.

**Giải pháp:**
Thêm deduplication filter trước khi render wallet history. Filter dựa trên key: `type|description|amount`.

**Code mới:**
```java
private List<Transaction> deduplicateSettlementTransactions(List<Transaction> transactions) {
    Set<String> seen = new HashSet<>();
    return transactions.stream()
            .filter(trans -> {
                String type = trans.getType();
                // Chỉ deduplicate settlement transactions
                if (!"BID_PAYMENT".equals(type) && !"BID_SUCCESS".equals(type) 
                    && !"AUCTION_SALE".equals(type)) {
                    return true;
                }
                // Extract session marker from description
                String desc = trans.getDescription() == null ? "" : trans.getDescription();
                if (desc.startsWith("#")) {
                    int firstSpace = desc.indexOf(' ');
                    if (firstSpace > 0) {
                        desc = desc.substring(0, firstSpace);
                    }
                }
                String key = type + "|" + desc + "|" + String.format("%.2f", trans.getAmount());
                return seen.add(key);
            })
            .toList();
}
```

**Trong `renderWalletData()`:**
```java
List<Transaction> transactions = deduplicateSettlementTransactions(data.transactions());
for (Transaction trans : transactions) {
    // render card...
}
```

### 7. Transaction type convention mới

**Trước đây:**
- Cả buyer và seller đều dùng type `BID_SUCCESS`
- UI phải đoán dựa vào role hiện tại → sai khi user vừa là bidder vừa là seller

**Bây giờ:**
- Buyer payment: `BID_PAYMENT` (trừ tiền, hiển thị đỏ)
- Seller income: `AUCTION_SALE` (cộng tiền, hiển thị xanh)
- Legacy `BID_SUCCESS` vẫn được xử lý như `BID_PAYMENT` để tương thích data cũ

**Code trong `TransHisCardController`:**
```java
if (type.equals("BID_PAYMENT") || type.equals("BID_SUCCESS")) {
    lblTransTit.setText("Successfully bid for " + desc);
    lblTransAmount.setText("-$" + String.format("%,.2f", amount));
    lblTransAmount.setStyle("-fx-text-fill: red;");
} else if (type.equals("AUCTION_SALE")) {
    lblTransTit.setText("Auction sale for " + desc);
    lblTransAmount.setText("+$" + String.format("%,.2f", amount));
    lblTransAmount.setStyle("-fx-text-fill: green;");
}
```

---

## ❓ FAQ cho Duy

**Q: Nếu thêm session mới, khi nào lock object trong `SESSION_BID_LOCKS` bị xóa?**  
A: Hiện tại CHƯA xóa (memory leak nhỏ). Với quy mô project sinh viên thì OK. Production cần cleanup.

**Q: Tại sao test dùng Mockito thay vì DB thật?**  
A: Unit test phải nhanh + không phụ thuộc external. Mock cho phép test logic riêng biệt, không cần MySQL chạy. DAO test dùng H2 in-memory.

**Q: `PriorityBlockingQueue` khác `PriorityQueue` gì?**  
A: `PriorityBlockingQueue` thread-safe (nhiều thread có thể add/poll cùng lúc). `PriorityQueue` không thread-safe → crash khi concurrent access.

**Q: Tại sao `processAutoBids` break sau 1 bid?**  
A: Tránh ping-pong: A auto-bid → B auto-bid → A auto-bid → ... → giá tăng không kiểm soát. Break → BidService gọi `processAutoBids` lại ở lần bid tiếp → kiểm soát từng bước.

**Q: Test thất bại "ít nhất 50% bid phải thành công" nghĩa là gì?**  
A: Stress test tạo 500 bid đồng thời. Nếu < 50% thành công → có vấn đề nghiêm trọng (deadlock, data corruption). Con số 50% là threshold tối thiểu — thực tế thường > 80%.
