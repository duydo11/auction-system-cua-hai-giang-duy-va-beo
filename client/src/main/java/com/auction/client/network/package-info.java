/**
 * <p>Stack socket client — RPC request/response và nhận push từ server.</p>
 * <ul>
 *   <li>{@link com.auction.client.network.ClientConnection}: singleton giữ host/port và vòng đời kết nối.</li>
 *   <li>{@link com.auction.client.network.SocketClient}: luồng đọc riêng, pending theo {@code correlationId},
 *       push {@code AUCTION_*_PUSH} chuyển sang UI.</li>
 *   <li>{@link com.auction.client.network.ClientProtocolHandler}: facade RPC cho controller JavaFX.</li>
 *   <li>{@link com.auction.client.network.NetworkCleanup}: logout — đóng socket, xóa session.</li>
 * </ul>
 * <p>Observer (rubric): UI đăng ký listener trên {@link com.auction.client.RealtimeAuctionBus}
 * (push giá/phiên mới không cần F5).</p>
 */
package com.auction.client.network;
