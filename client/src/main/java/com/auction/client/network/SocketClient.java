package com.auction.client.network;

import com.auction.shared.protocol.Message;
import java.io.*;
import java.net.Socket;

public class SocketClient {
    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private volatile boolean isConnected;

    public SocketClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.isConnected = false;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);

            // Khởi tạo streams (OutputStream trước, rồi InputStream)
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(socket.getInputStream());

            isConnected = true;
            System.out.println("✓ Connected to server: " + serverHost + ":" + serverPort);
            return true;
        } catch (IOException e) {
            System.err.println("✗ Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public Message sendMessage(Message message) {
        if (!isConnected) {
            System.err.println("✗ Not connected to server");
            return null;
        }

        try {
            System.out.println("→ Sending to server: " + message.getType());
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();

            // Đợi response từ server
            Message response = (Message) objectInputStream.readObject();
            System.out.println("← Received from server: " + response.getType());
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("✗ Error sending message: " + e.getMessage());
            isConnected = false;
            return null;
        }
    }

    public void disconnect() {
        try {
            isConnected = false;
            if (objectInputStream != null) objectInputStream.close();
            if (objectOutputStream != null) objectOutputStream.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("✓ Disconnected from server");
        } catch (IOException e) {
            System.err.println("✗ Error disconnecting: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}