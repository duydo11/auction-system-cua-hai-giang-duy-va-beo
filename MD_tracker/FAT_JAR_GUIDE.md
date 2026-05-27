# Hướng Dẫn Build & Chạy Fat JAR

Build ra file `.jar` chứa toàn bộ thư viện, chạy bằng lệnh `java -jar` không cần cài thêm gì.

---

## Build

### Cách 1: Dùng script (khuyến nghị)

```bat
build-fat-jar.bat
```

### Cách 2: Maven trực tiếp

```bash
mvn clean package -DskipTests
```

Thời gian build: ~30-40 giây.

---

## Output

Sau khi build xong, 2 file JAR được tạo tại:

```
server\target\server-1.0-SNAPSHOT-jar-with-dependencies.jar   (~5 MB)
client\target\client-1.0-SNAPSHOT-jar-with-dependencies.jar   (~70 MB)
```

> Client JAR lớn hơn vì chứa toàn bộ JavaFX runtime (javafx-controls, javafx-fxml, javafx-graphics, javafx-base) và Kotlin stdlib.

---

## Chạy

### Bước 1 — Khởi động server

Mở terminal, chạy:

```bash
java -jar server\target\server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Chờ thấy dòng này thì server sẵn sàng:

```
🚀 Listening TCP on 0.0.0.0:5000 — clients dùng cùng port trong client.properties
```

> **Giữ terminal này mở**, đừng đóng.

### Bước 2 — Khởi động client

Mở **terminal mới** (terminal khác với terminal server), chạy:

```bash
java -jar client\target\client-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Giao diện JavaFX sẽ hiện lên. Đăng nhập bình thường.

---

## Yêu Cầu

- Java 21 trở lên
- Không cần cài JavaFX riêng (đã đóng gói sẵn trong JAR)
- Máy cần có kết nối internet (client kết nối server qua IP/port trong `client.properties`)

---

## Lưu Ý

- File JAR được build trên Windows. Chạy tốt trên Windows. Nếu cần chạy trên Linux/macOS thì build lại trên đúng máy đó.
- Mỗi lần sửa code phải build lại JAR.
- Thư mục `target/` có thể bị xóa bởi `mvn clean`, nhớ build lại sau đó.

---

## Cấu Hình Plugin (Tham Khảo)

Plugin đã được thêm vào `server/pom.xml` và `client/pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <archive>
            <manifest>
                <mainClass><!-- main class --></mainClass>
            </manifest>
        </archive>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

| Module | mainClass |
|--------|-----------|
| server | `com.auction.server.ServerMain` |
| client | `com.auction.client.Launcher` |
