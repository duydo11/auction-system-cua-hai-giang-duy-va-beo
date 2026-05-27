package com.auction.client.util;

import com.auction.shared.model.auction.AuctionSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cache in-memory cho auction ở client.
 *
 * <p>QUAN TRỌNG: không dùng chung một list cho mọi màn nữa.</p>
 * <ul>
 *   <li>activeAuctions: chỉ chứa phiên đang có thể đặt giá, dùng cho bidder dashboard/items.</li>
 *   <li>allAuctions: chứa toàn bộ lịch sử, dùng cho seller My Listings và admin.</li>
 * </ul>
 *
 * <p>Nếu trộn 2 loại dữ liệu này, màn bidder có thể ghi cache chỉ còn vài phiên active,
 * rồi seller/admin đọc lại cache đó và tưởng lịch sử bị mất.</p>
 */
public final class AuctionCache {

    /** Cache chỉ để render tức thì; các màn vẫn refresh nền từ server. */
    private static volatile List<AuctionSession> activeAuctions = null;
    private static volatile List<AuctionSession> allAuctions = null;

    private AuctionCache() {
    }

    public static boolean hasActiveData() {
        return activeAuctions != null;
    }

    public static boolean hasAllData() {
        return allAuctions != null;
    }

    /** Backward-compatible: coi như có dữ liệu nếu một trong hai cache đã có. */
    public static boolean hasData() {
        return hasActiveData() || hasAllData();
    }

    public static List<AuctionSession> getActive() {
        List<AuctionSession> snapshot = activeAuctions;
        return snapshot != null ? snapshot : Collections.emptyList();
    }

    public static List<AuctionSession> getAll() {
        List<AuctionSession> snapshot = allAuctions;
        return snapshot != null ? snapshot : Collections.emptyList();
    }

    /** Backward-compatible: ưu tiên all, nếu chưa có thì trả active. */
    public static List<AuctionSession> get() {
        if (allAuctions != null) {
            return allAuctions;
        }
        return getActive();
    }

    /** Không còn dùng TTL 30s để tránh giữ dữ liệu cũ quá lâu khi test nhiều cửa sổ. */
    public static boolean isStale() {
        return true;
    }

    public static void updateActive(List<AuctionSession> auctions) {
        activeAuctions = safeCopy(auctions);
    }

    public static void updateAll(List<AuctionSession> auctions) {
        allAuctions = safeCopy(auctions);
    }

    /** Backward-compatible: mặc định update all để không làm mất lịch sử. */
    public static void update(List<AuctionSession> auctions) {
        updateAll(auctions);
    }

    public static void invalidateActive() {
        activeAuctions = null;
    }

    public static void invalidateAll() {
        allAuctions = null;
    }

    public static void invalidate() {
        invalidateActive();
        invalidateAll();
    }

    /** Thêm/cập nhật auction vào cả hai cache nếu cache đó đang tồn tại. */
    public static synchronized void addOrReplace(AuctionSession auction) {
        if (auction == null) {
            return;
        }
        if (activeAuctions != null) {
            activeAuctions = addOrReplaceIn(activeAuctions, auction);
        }
        if (allAuctions != null) {
            allAuctions = addOrReplaceIn(allAuctions, auction);
        }
    }

    /** Xóa auction khỏi cả active/all cache, dùng sau khi admin xóa sản phẩm. */
    public static synchronized void removeBySessionId(int sessionId) {
        if (activeAuctions != null) {
            activeAuctions = removeFrom(activeAuctions, sessionId);
        }
        if (allAuctions != null) {
            allAuctions = removeFrom(allAuctions, sessionId);
        }
    }

    public static synchronized void removeByItemId(int itemId) {
        if (activeAuctions != null) {
            activeAuctions = activeAuctions.stream()
                    .filter(s -> s.getItem() == null || s.getItem().getId() != itemId)
                    .toList();
        }
        if (allAuctions != null) {
            allAuctions = allAuctions.stream()
                    .filter(s -> s.getItem() == null || s.getItem().getId() != itemId)
                    .toList();
        }
    }

    private static List<AuctionSession> safeCopy(List<AuctionSession> auctions) {
        return auctions == null ? Collections.emptyList() : new ArrayList<>(auctions);
    }

    private static List<AuctionSession> addOrReplaceIn(List<AuctionSession> source, AuctionSession auction) {
        List<AuctionSession> next = new ArrayList<>(source);
        next.removeIf(existing -> existing.getId() == auction.getId());
        next.add(0, auction);
        return next;
    }

    private static List<AuctionSession> removeFrom(List<AuctionSession> source, int sessionId) {
        return source.stream()
                .filter(session -> session.getId() != sessionId)
                .toList();
    }
}
