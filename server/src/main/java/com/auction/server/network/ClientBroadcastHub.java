package com.auction.server.network;

import com.auction.shared.protocol.Message;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gửi push tới mọi client đang kết nối (realtime, không polling).
 */
public final class ClientBroadcastHub {

    private static final Set<ClientHandler> handlers = ConcurrentHashMap.newKeySet();

    private ClientBroadcastHub() {
    }

    public static void register(ClientHandler handler) {
        handlers.add(handler);
    }

    public static void unregister(ClientHandler handler) {
        handlers.remove(handler);
    }

    public static void broadcast(Message push) {
        for (ClientHandler h : handlers) {
            h.deliverPush(push);
        }
    }
}
