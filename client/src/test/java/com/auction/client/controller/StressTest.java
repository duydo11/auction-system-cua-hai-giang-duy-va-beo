package com.auction.client.controller;

//public class StressTest {
//    public static void main(String[] args) {
//        System.out.println("🚀 BẮT ĐẦU DỘI BOM SERVER VỚI 100 LUỒNG CÙNG LÚC...");
//
//        // Tạo 100 luồng (giả lập 100 ông User đang canh me giây cuối cùng)
//        for (int i = 0; i < 100; i++) {
//            int userId = i;
//            new Thread(() -> {
//                try {
//                    // Mở Socket kết nối
//                    ClientConnection conn = new ClientConnection("localhost", 5000);
//                    conn.connect();
//
//                    // Gửi lệnh Bid giá tăng dần
//                    Message bidMsg = new Message(TrayIcon.MessageType.PLACE_BID_REQUEST,
//                            new BidData(1, userId, 500.0 + userId));
//                    conn.sendMessage(bidMsg);
//
//                } catch (Exception e) {}
//            }).start();
//        }
//    }
//}