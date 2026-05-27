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

### `placeBid()` — Logic đầy đủ:

```java
public boolean placeBid(int sessionId, int bidderId, double amount) {
    synchronized (lockForSession(sessionId)) {
        // ==== Chỉ 1 thread vào đây cùng lúc (cho cùng sessionId) ====
        
        // 1. Load session từ DB (mới nhất)
        AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
        if (session == null) return false;
        
        // 2. Load bidder từ DB
        User bidder = userDAO.getUserById(bidderId);
        if (bidder == null) return false;
        
        // 3. Tạo Bid object
        int bidId = bidDAO.allocateNextBidId();
        Bid bid = new Bid(bidId, bidder, session, amount);
        
        // 4. Ghi nhớ số lượng bid TRƯỚC khi thử
        int beforeCount = session.getBids().size();
        
        // 5. Thử cập nhật giá (validate bên trong AuctionSession)
        session.updateCurrentPrice(bid);
        // updateCurrentPrice kiểm tra:
        //   - session.isActive() (thời gian + status)
        //   - amount > currentPrice
        //   - time trong phạm vi start-end
        
        // 6. Nếu bids.size() tăng → bid được chấp nhận
        if (session.getBids().size() > beforeCount) {
            bidDAO.saveBid(bid, sessionId);           // Lưu bid vào DB
            checkAndExtendForAntiSnipe(session);      // Check anti-snipe
            auctionSessionDAO.updateSession(session);  // Cập nhật phiên
            return true;
        }
        
        // 7. Không tăng → bid bị reject (giá thấp, hết giờ,...)
        return false;
    }
}
```

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
    PriorityBlockingQueue<AutoBidConfig> queue = autoBidsBySession.get(sessionId);
    if (queue == null || queue.isEmpty()) return false;
    
    AuctionSession session = auctionSessionDAO.getSessionById(sessionId);
    if (session == null || !session.isActive()) return false;
    
    List<AutoBidConfig> toRequeue = new ArrayList<>();
    
    while (!queue.isEmpty()) {
        AutoBidConfig config = queue.poll();  // Lấy ra (FIFO theo registeredAt)
        
        // Bỏ qua nếu auto-bidder đang thắng (không tự bid lại chính mình)
        if (session.getWinner() != null && session.getWinner().getId() == config.getBidderId()) {
            toRequeue.add(config);
            continue;
        }
        
        // Tính giá tiếp theo
        double currentPrice = session.getCurrentPrice();
        double nextBid = Math.min(currentPrice + config.getIncrement(), config.getMaxBid());
        
        // Nếu bid được (giá hợp lệ và chưa vượt maxBid)
        if (nextBid > currentPrice && nextBid <= config.getMaxBid()) {
            // Tạo bid tự động
            Bid autoBid = new Bid(bidDAO.allocateNextBidId(), 
                                  userDAO.getUserById(config.getBidderId()),
                                  session, nextBid);
            
            session.updateCurrentPrice(autoBid);
            bidDAO.saveBid(autoBid, sessionId);
            auctionSessionDAO.updateSession(session);
            
            // Broadcast update
            ClientBroadcastHub.broadcast(new Message(MessageType.AUCTION_UPDATED_PUSH, session));
            
            toRequeue.add(config);
            break;  // QUAN TRỌNG: Chỉ 1 auto-bid mỗi lần → tránh infinite loop
        } else {
            toRequeue.add(config);  // Giữ lại cho lần sau
        }
    }
    
    // Đưa tất cả config lại vào queue
    for (AutoBidConfig config : toRequeue) {
        queue.add(config);
    }
    
    return anyAutoBidTriggered;
}
```

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

---

## 🔄 Cập nhật source mới nhất Duy cần biết

### 1. Client RPC đã được serialize để tránh `Socket closed`

File chính: `client/src/main/java/com/auction/client/network/ClientProtocolHandler.java`

Client dùng `ClientConnection` singleton, nhưng nhiều controller có thể gọi request cùng lúc.
Để tránh controller này disconnect socket trong lúc controller khác đang write, `ClientProtocolHandler` hiện có lock chung:

```java
private static final Object RPC_LOCK = new Object();
```

Mọi RPC đi qua lock này. Cách này đơn giản, hy sinh một ít song song nhưng tăng ổn định cho app demo.

### 2. AutoBidService thay thế config cũ

File chính: `server/src/main/java/com/auction/server/service/AutoBidService.java`

`registerAutoBid(config)` hiện xóa config cũ của cùng bidder/session trước khi add config mới:

```java
queue.removeIf(existing -> existing.getBidderId() == config.getBidderId());
queue.add(config);
```

Mục đích:

- tránh một bidder có nhiều config trùng trên cùng phiên;
- tránh tick/start auto-bid nhiều lần làm server tự bid lặp bất thường.

### 3. Auction list không kéo bids trong dashboard

File chính: `server/src/main/java/com/auction/server/dao/AuctionSessionDAO.java`

Các hàm list đã tối ưu bằng summary `JOIN`, không gọi `getSessionById()` từng dòng nữa.
Điểm cần nhớ khi viết test/perf:

- list/dashboard nhanh hơn vì không load bid history;
- bid history chỉ load ở detail;
- nếu test kỳ vọng `session.getBids()` có đủ data từ list thì cần đổi test sang gọi `getSessionById()` hoặc `getBidHistory()`.

### 4. Bidder dashboard polling giảm tải

File chính: `client/src/main/java/com/auction/client/controller/BidderScene/BidderDashboardController.java`

Polling đã đổi từ 3 giây/lần sang 10 giây/lần để giảm tải DB cloud.
Realtime push vẫn cập nhật ngay khi có auction/bid update.

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
