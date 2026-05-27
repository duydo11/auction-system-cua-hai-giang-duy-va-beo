package com.auction.client.util;

import com.auction.shared.model.auction.AuctionSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cache in-memory cho danh sách auction đang hoạt động.
 *
 * <p>Dùng pattern <strong>stale-while-revalidate</strong>:
 * <ol>
 *   <li>Nếu cache đang có data → trả về ngay (UI render tức thì, không cần loading).</li>
 *   <li>Sau đó refresh ngầm ở background để data luôn gần đúng.</li>
 *   <li>Nếu cache rỗng (lần đầu mở app) → phải đợi fetch lần đầu.</li>
 * </ol>
 *
 * <p>Cache tự động hết hạn sau {@value #TTL_MS} ms. Sau khi hết hạn, lần fetch tiếp theo
 * vẫn trả cache cũ nhưng đồng thời kick-off refresh ngầm để không làm chậm UI.</p>
 *
 * <p>Cache cũng có thể bị invalidate thủ công (ví dụ sau khi tạo auction mới)
 * bằng cách gọi {@link #invalidate()}.</p>
 */
public final class AuctionCache {

    /** Thời gian cache còn hiệu lực: 30 giây. */
    private static final long TTL_MS = 30_000;

    private static volatile List<AuctionSession> cached = null;
    private static volatile long fetchedAtMs = 0L;

    private AuctionCache() {
    }

    /**
     * Kiểm tra cache có data hay chưa (kể cả data đã stale).
     *
     * @return true nếu có ít nhất một lần fetch thành công trước đó
     */
    public static boolean hasData() {
        return cached != null;
    }

    /**
     * Trả về data hiện có trong cache (có thể đã stale).
     * Trả về list rỗng nếu chưa có data lần nào.
     */
    public static List<AuctionSession> get() {
        List<AuctionSession> snapshot = cached;
        return snapshot != null ? snapshot : Collections.emptyList();
    }

    /**
     * Kiểm tra cache đã hết hạn hay chưa.
     *
     * @return true nếu cache rỗng hoặc đã quá {@value #TTL_MS} ms
     */
    public static boolean isStale() {
        return cached == null || (System.currentTimeMillis() - fetchedAtMs) > TTL_MS;
    }

    /**
     * Cập nhật cache với data mới từ server.
     *
     * @param auctions danh sách auction mới nhất từ server
     */
    public static void update(List<AuctionSession> auctions) {
        cached = auctions;
        fetchedAtMs = System.currentTimeMillis();
    }

    /**
     * Xóa cache, buộc fetch lại lần kế tiếp.
     *
     * <p>Gọi sau các thao tác có thể làm thay đổi danh sách auction,
     * ví dụ: tạo auction mới, kết thúc auction.</p>
     */
    public static void invalidate() {
        cached = null;
        fetchedAtMs = 0L;
    }

    public static synchronized void addOrReplace(AuctionSession auction) {
        if (auction == null) {
            return;
        }
        List<AuctionSession> next = new ArrayList<>(get());
        if (auction.getId() <= 0) {
            int maxId = next.stream().mapToInt(AuctionSession::getId).max().orElse(0);
            auction.setId(maxId + 1);
        }
        next.removeIf(existing -> existing.getId() == auction.getId());
        next.add(0, auction);
        update(next);
    }
}
