package com.auction.server.network;

import com.auction.server.config.DatabaseConfig;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private volatile boolean isRunning;

    public SocketServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
        this.isRunning = false;
    }

    public SocketServer() {
        this(DatabaseConfig.getServerPort());
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("✓ Server started on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✓ New client connected: " + clientSocket.getInetAddress());

                threadPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("✗ Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("✓ Server stopped");
        } catch (IOException e) {
            System.err.println("✗ Error closing server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SocketServer server = new SocketServer();
        server.start();
    }
}