package com.auction.client.ui;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.client.util.FxAsync;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.User;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Factory tạo các row hiển thị auction trong UI.
 *
 * <p>Các thao tác network (như placeBid) được chạy ở background để UI không bị đứng
 * khi chờ server phản hồi.</p>
 */
public final class  AuctionRowFactory {

    private AuctionRowFactory() {
    }

    /**
     * Tạo một dòng bid: hiển thị thông tin phiên, ô nhập giá, và nút đặt giá.
     *
     * <p>Thao tác đặt giá chạy async để user không bị đơ khi chờ server.</p>
     *
     * @param session   phiên đấu giá
     * @param protocol  handler để gọi server
     * @param onSuccess callback chạy sau khi đặt giá thành công
     * @return HBox chứa row
     */
    public static HBox bidRow(AuctionSession session, ClientProtocolHandler protocol, Runnable onSuccess) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(4));
        String itemName = session.getItem() != null ? session.getItem().getName() : "—";
        row.getChildren().add(new Label("#" + session.getId() + " · " + itemName));
        row.getChildren().add(new Label(String.format("Price: %.2f", session.getCurrentPrice())));
        TextField tf = new TextField();
        tf.setPromptText("Bid amount");
        tf.setPrefWidth(110);
        Button btn = new Button("Place Bid");
        Label msg = new Label();
        msg.setStyle("-fx-text-fill: #c62828;");

        // Xử lý đặt giá ở background thread
        btn.setOnAction(ev -> {
            User u = SessionContext.getCurrentUser();
            if (u == null) {
                msg.setText("Not logged in");
                return;
            }

            String txt = tf.getText().trim().replace(",", "");
            double amt;
            try {
                amt = Double.parseDouble(txt);
            } catch (NumberFormatException ex) {
                msg.setText("Invalid number");
                return;
            }

            // Disable button và hiện trạng thái đang gửi
            btn.setDisable(true);
            String originalText = btn.getText();
            btn.setText("Sending...");

            // Gọi placeBid ở thread nền
            FxAsync.run("bid-" + session.getId(),
                    () -> protocol.placeBidOrError(session.getId(), u.getId(), amt),
                    error -> {
                        if (error == null) {
                            msg.setText("");
                            tf.clear();
                            onSuccess.run();
                        } else {
                            msg.setText(error);
                        }
                        // Khôi phục button
                        btn.setDisable(false);
                        btn.setText(originalText);
                    });
        });
        row.getChildren().addAll(tf, btn, msg);
        return row;
    }
}
