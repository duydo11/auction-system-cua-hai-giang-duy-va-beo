package com.auction.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private static final int PORT = 8080;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 Server đang chạy ở cổng " + PORT + ". Đang chờ Client...");

            // Vòng lặp vô hạn để đón nhiều khách

            while (true){
                Socket clientSocket = serverSocket.accept();
                System.out.println(("Co Client moi ket noi: " + clientSocket.getInetAddress()));
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();

            }
        } catch (IOException e){
            System.err.println("loi khoi dong Sever: "+ e.getMessage());
        }
    }
}