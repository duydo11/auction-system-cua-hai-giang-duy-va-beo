package com.auction.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Giữ một {@link SocketClient} và vòng đời kết nối (singleton).
 */
public final class ClientConnection {
    private static volatile ClientConnection instance;

    private final String host;
    private final int port;
    private SocketClient socketClient;

    private ClientConnection() {
        this(readHost(), readPort());
    }

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static ClientConnection getInstance() {
        if (instance == null) {
            synchronized (ClientConnection.class) {
                if (instance == null) {
                    instance = new ClientConnection();
                }
            }
        }
        return instance;
    }

    private static String readHost() {
        try (InputStream in = ClientConnection.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (in == null) {
                return "localhost";
            }
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("server.host", "localhost");
        } catch (IOException e) {
            return "localhost";
        }
    }

    private static int readPort() {
        try (InputStream in = ClientConnection.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (in == null) {
                return 5000;
            }
            Properties p = new Properties();
            p.load(in);
            return Integer.parseInt(p.getProperty("server.port", "5000"));
        } catch (IOException | NumberFormatException e) {
            return 5000;
        }
    }

    public boolean connect() {
        if (socketClient != null && socketClient.isConnected()) {
            return true;
        }
        socketClient = new SocketClient(host, port);
        return socketClient.connect();
    }

    public SocketClient getSocketClient() {
        return socketClient;
    }

    public void disconnect() {
        if (socketClient != null) {
            socketClient.disconnect();
            socketClient = null;
        }
    }

    public boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
