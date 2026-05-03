package com.auction.server.network;

import com.auction.shared.protocol.Message;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ServerProtocolHandler protocolHandler = new ServerProtocolHandler();
    private final Object writeLock = new Object();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    void deliverPush(Message push) {
        try {
            sendLocked(push);
        } catch (IOException e) {
            System.err.println("✗ Push failed for client: " + clientSocket.getInetAddress());
        }
    }

    private void sendLocked(Message m) throws IOException {
        synchronized (writeLock) {
            objectOutputStream.writeObject(m);
            objectOutputStream.flush();
        }
    }

    @Override
    public void run() {
        try {
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            System.out.println("✓ Streams initialized for " + clientSocket.getInetAddress());

            ClientBroadcastHub.register(this);

            while (true) {
                Message message = (Message) objectInputStream.readObject();

                if (message == null) break;

                System.out.println("→ Received from client: " + message.getType());

                Message response = protocolHandler.handleMessage(message);

                sendLocked(response);

                System.out.println("← Sent to client: " + response.getType());
            }
        } catch (EOFException e) {
            System.out.println("✓ Client disconnected: " + clientSocket.getInetAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("✗ Error handling client: " + e.getMessage());
        } finally {
            ClientBroadcastHub.unregister(this);
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
