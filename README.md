# Auction System - BTL LTNC

He thong dau gia online theo mo hinh `client-server-shared`:
- `client`: JavaFX desktop app cho nguoi dung.
- `server`: TCP socket server xu ly nghiep vu va truy cap MySQL.
- `shared`: model/protocol dung chung giua client va server.

Muc tieu: dang ky/dang nhap, tao phien dau gia, xem danh sach phien, dat gia theo thoi gian thuc (push qua socket), xem lich su bid.

## 1) Kien truc tong quan

```
JavaFX Client <--> Socket TCP (Message + MessageType) <--> Server Service/DAO <--> MySQL
                       ^
                       | (shared module: model, protocol, enum)
```

- Client gui request theo `MessageType` (login, view auctions, place bid...).
- Server tra response theo `correlationId`, dong thoi push realtime (`AUCTION_UPDATED_PUSH`, `AUCTION_CREATED_PUSH`) cho client dang online.
- `shared` chua class dung chung de tranh lech schema giua hai phia.

## 2) Tinh nang chinh theo vai tro

### Bidder
- Dang ky / dang nhap.
- Xem danh sach phien dau gia dang mo theo danh muc.
- Dat gia vao phien.
- Theo doi cap nhat gia realtime.
- Xem lich su dat gia (theo phien).

### Seller
- Dang nhap vao seller dashboard.
- Tao phien dau gia moi (item + thong tin gia/mo-ta/thoi gian).
- Theo doi cap nhat phien.

### Admin
- Co model/bang `admins` trong DB.
- Hien tai UI chua co dashboard rieng; role `ADMIN` dang duoc dieu huong ve man hinh bidder dashboard.

## 3) Yeu cau moi truong

- JDK: **21**
- Maven: **3.9+**
- MySQL: **8.x**
- OS da test: Windows (co the chay tren Linux/macOS neu cai JavaFX phu hop)

## 4) Cau hinh nhanh

### Database
1. Tao schema va bang:
   - Cach 1: import `database/db.sql`
   - Cach 2: chay script `server/src/main/resources/AuctionDatabase.sql`
2. Dong bo ten DB trong `server/src/main/resources/config.properties`:
   - Mac dinh hien tai: `db.name=auction_system`
   - Luu y script SQL dang tao `auction_db` -> can chinh mot trong hai de trung nhau.

### Server socket
- File: `server/src/main/resources/config.properties`
- Bien quan trong:
  - `server.host` (mac dinh `0.0.0.0`)
  - `server.port` (mac dinh `5000`)

### Client socket
- File: `client/src/main/resources/client.properties`
- Dat cung port voi server:
  - `server.host=127.0.0.1`
  - `server.port=5000`

## 5) Cach chay nhanh tung module

Tai root repo:

```bash
mvn clean compile
```

### Chay server

```bash
mvn -pl server exec:java -Dexec.mainClass=com.auction.server.ServerMain
```

Lenh tren da duoc cau hinh san trong `server/pom.xml` (`exec-maven-plugin`),
co the rut gon:

```bash
mvn -pl server exec:java
```

### Chay client (JavaFX)

```bash
mvn -pl client javafx:run
```

Hoac chay class trong IDE:
- `com.auction.client.Launcher`

## 6) Test va CI

### Chay test local

```bash
mvn test
```

Test hien co chu yeu o module server (DAO test), co the can DB san du lieu/cau hinh dung.

### CI hien co
- Workflow: `.github/workflows/maven.yml`
- Trigger: `push`/`pull_request` vao `main`, `develop`
- Buoc chinh:
  - `mvn clean compile`
  - `mvn test`

## 7) Demo flow ngan (test thu cong)

1. Import DB va cap nhat `config.properties`.
2. Run server, dam bao log hien `Listening TCP`.
3. Run client.
4. Dang ky tai khoan bidder/seller.
5. Dang nhap seller -> tao phien dau gia.
6. Dang nhap bidder (co the o client instance khac) -> vao Items -> dat gia.
7. Kiem tra cap nhat realtime va lich su bid.

## 8) Xu ly loi socket/runtime (ban hien tai)

- Client co timeout ket noi/doc socket de tranh treo im lang.
- Co retry 1 lan o muc protocol khi request that bai do mat ket noi tam thoi.
- Khi socket bi dong/ghi that bai: fail cac request dang cho + log loi ro nguyen nhan.
- UI trang Items hien thi trang thai `(offline)` khi khong the lay du lieu tu server.

## 9) Known issues / han che

- Chua co co che reconnect nen cap (backoff nhieu lan, queue offline).
- Chua co man hinh/admin workflow rieng du role `ADMIN` ton tai trong model/DB.
- Mot so test DAO phu thuoc DB that, chua mock/embedded DB.
- Ten DB trong SQL script va `config.properties` co the khac nhau neu khong dong bo.
