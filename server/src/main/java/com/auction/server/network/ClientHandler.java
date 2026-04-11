package com.auction.server.network;

<<<<<<< Updated upstream
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.model.user.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Request request = (Request) in.readObject();
                System.out.println("Server nhận yêu cầu: " + request.getAction());

                Response response = processRequest(request);

                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client ngắt kết nối.");
        } finally {
            closeConnections();
        }
    }

    private Response processRequest(Request request) {
        if ("LOGIN".equals(request.getAction())) {
            // Ép kiểu về User (Class bạn đã viết trong shared)
            User user = (User) request.getPayload();

            // Chỗ này tạm thời hardcode, sau này gọi UserDAO của Hải để check DB
            if ("hoang".equals(user.getUsername()) && "123".equals(user.getPassword())) {
                return new Response("SUCCESS", "Đăng nhập thành công", user);
            }
            return new Response("ERROR", "Sai tài khoản hoặc mật khẩu", null);
        }
        return new Response("ERROR", "Không rõ hành động", null);
    }

    private void closeConnections() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
=======
public class ClientHandler {
}
>>>>>>> Stashed changes
