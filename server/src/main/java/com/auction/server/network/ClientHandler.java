package com.auction.server.network;

import com.auction.shared.protocol.Message;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ServerProtocolHandler protocolHandler;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.protocolHandler = new ServerProtocolHandler();
    }

    @Override
    public void run() {
        try {
            // Khởi tạo streams (OutputStream trước, rồi InputStream)
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            System.out.println("✓ Streams initialized for " + clientSocket.getInetAddress());

            // Lắng nghe messages từ client
            while (true) {
                Message message = (Message) objectInputStream.readObject();

                if (message == null) break;

                System.out.println("→ Received from client: " + message.getType());

                // Xử lý message
                Message response = protocolHandler.handleMessage(message);

                // Gửi response lại cho client
                objectOutputStream.writeObject(response);
                objectOutputStream.flush();

                System.out.println("← Sent to client: " + response.getType());
            }
        } catch (EOFException e) {
            System.out.println("✓ Client disconnected: " + clientSocket.getInetAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("✗ Error handling client: " + e.getMessage());
        } finally {
            closeResources();
        }
    }

    private void closeResources() {
        try {
            if (objectInputStream != null) objectInputStream.close();
            if (objectOutputStream != null) objectOutputStream.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("✓ Resources closed for client");
        } catch (IOException e) {
            System.err.println("✗ Error closing resources: " + e.getMessage());
        }
    }
}
