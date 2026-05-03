package com.auction.client.ui;

import com.auction.client.SessionContext;
import com.auction.client.network.ClientProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.user.User;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public final class AuctionRowFactory {

    private AuctionRowFactory() {
    }

    /** Một dòng: thông tin phiên + ô nhập giá + nút đặt (gọi server qua {@link ClientProtocolHandler}). */
    public static HBox bidRow(AuctionSession session, ClientProtocolHandler protocol, Runnable onSuccess) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(4));
        String itemName = session.getItem() != null ? session.getItem().getName() : "—";
        row.getChildren().add(new Label("#" + session.getId() + " · " + itemName));
        row.getChildren().add(new Label(String.format("Giá: %.2f", session.getCurrentPrice())));
        TextField tf = new TextField();
        tf.setPromptText("Giá đặt");
        tf.setPrefWidth(110);
        Button btn = new Button("Đặt giá");
        Label msg = new Label();
        msg.setStyle("-fx-text-fill: #c62828;");
        btn.setOnAction(ev -> {
            User u = SessionContext.getCurrentUser();
            if (u == null) {
                msg.setText("Chưa đăng nhập");
                return;
            }
            try {
                double amt = Double.parseDouble(tf.getText().trim().replace(",", ""));
                String err = protocol.placeBidOrError(session.getId(), u.getId(), amt);
                if (err == null) {
                    msg.setText("");
                    tf.clear();
                    onSuccess.run();
                } else {
                    msg.setText(err);
                }
            } catch (NumberFormatException ex) {
                msg.setText("Số không hợp lệ");
            }
        });
        row.getChildren().addAll(tf, btn, msg);
        return row;
    }
}
