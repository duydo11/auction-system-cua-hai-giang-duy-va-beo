/**
 * <p>Lớp mạng phía server — minh chứng kiến trúc client-server & xử lý đa luồng (rubric).</p>
 * <ul>
 *   <li>{@link com.auction.server.network.SocketServer}: {@link java.net.ServerSocket}, chấp nhận kết nối,
 *       ủy quyền mỗi socket cho một luồng trong pool.</li>
 *   <li>{@link com.auction.server.network.ClientHandler}: một luồng đọc/ghi Object stream cho một client;
 *       {@code writeLock} serialize phản hồi RPC và push.</li>
 *   <li>{@link com.auction.server.network.ClientBroadcastHub}: đăng ký handler,
 *       {@link java.util.concurrent.ConcurrentHashMap} — broadcast push realtime tới mọi client.</li>
 *   <li>Luồng xử lý tin nhắn → {@link com.auction.server.network.ServerProtocolHandler} → service/DAO.</li>
 * </ul>
 */
package com.auction.server.network;
