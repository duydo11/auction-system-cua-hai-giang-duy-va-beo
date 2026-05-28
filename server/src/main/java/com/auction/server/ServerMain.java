package com.auction.server;

import com.auction.server.network.SocketServer;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("🚀 Auction Server starting…");
        System.out.println("(Giữ Run này chạy; mở client sau khi thấy dòng \"Listening\")");
        com.auction.server.service.AuctionScheduler.getInstance().start();
        SocketServer server = new SocketServer();
        server.start();
    }
}
