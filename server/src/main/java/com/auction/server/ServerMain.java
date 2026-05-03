package com.auction.server;

import com.auction.server.network.SocketServer;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Auction Server...");
        SocketServer server = new SocketServer();
        server.start();
    }
}
