package com.auction.server.tools;

import com.auction.server.network.ServerProtocolHandler;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.ItemFactory;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Smoke test chạy trực tiếp qua ServerProtocolHandler để kiểm tra backend/DB không phụ thuộc UI JavaFX.
 */
public class ProtocolSmokeTest {
    public static void main(String[] args) {
        ServerProtocolHandler handler = new ServerProtocolHandler();

        User user = loginOrRegister(handler, "smoke_user", "smoke123", "smoke_user@auction.local");
        if (user == null) {
            throw new IllegalStateException("Cannot login/register smoke user");
        }

        Seller seller = new Seller(user.getId(), user.getUsername(), user.getPassword(), user.getEmail(), 0.0);
        Item item = ItemFactory.create("Others", 0,
                "Smoke Test Item " + System.currentTimeMillis(),
                "Created by ProtocolSmokeTest", seller, null);
        AuctionSession session = new AuctionSession(0, seller, item, 100_000,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusHours(1));

        Message create = handler.handleMessage(new Message(MessageType.CREATE_AUCTION_REQUEST, session));
        requireSuccess(create, "CREATE_AUCTION_REQUEST");

        Message active = handler.handleMessage(new Message(MessageType.VIEW_AUCTIONS_REQUEST, null));
        int activeCount = countList(active, "VIEW_AUCTIONS_REQUEST");
        if (activeCount <= 0) {
            throw new IllegalStateException("Expected at least 1 active auction, got " + activeCount);
        }

        Message allAuctions = handler.handleMessage(new Message(MessageType.GET_ALL_AUCTIONS_REQUEST, null));
        int allAuctionCount = countList(allAuctions, "GET_ALL_AUCTIONS_REQUEST");
        if (allAuctionCount <= 0) {
            throw new IllegalStateException("Expected at least 1 total auction, got " + allAuctionCount);
        }

        Message users = handler.handleMessage(new Message(MessageType.GET_ALL_USERS_REQUEST, null));
        int userCount = countList(users, "GET_ALL_USERS_REQUEST");
        if (userCount <= 0) {
            throw new IllegalStateException("Expected at least 1 user, got " + userCount);
        }

        System.out.println("SMOKE_TEST_PASS activeAuctions=" + activeCount
                + " allAuctions=" + allAuctionCount + " users=" + userCount);
    }

    private static User loginOrRegister(ServerProtocolHandler handler, String username, String password, String email) {
        Message login = handler.handleMessage(new Message(MessageType.LOGIN_REQUEST, new String[]{username, password}));
        if (login != null && login.getData() instanceof User user) {
            return user;
        }

        Message register = handler.handleMessage(new Message(MessageType.REGISTER_REQUEST,
                new String[]{username, password, email, "BIDDER"}));
        requireSuccess(register, "REGISTER_REQUEST");

        Message loginAfterRegister = handler.handleMessage(new Message(MessageType.LOGIN_REQUEST, new String[]{username, password}));
        if (loginAfterRegister != null && loginAfterRegister.getData() instanceof User user) {
            return user;
        }
        return null;
    }

    private static void requireSuccess(Message message, String operation) {
        if (message == null || !message.isSuccess()) {
            throw new IllegalStateException(operation + " failed: " + describe(message));
        }
    }

    private static int countList(Message message, String operation) {
        requireSuccess(message, operation);
        if (message.getData() instanceof List<?> list) {
            return list.size();
        }
        throw new IllegalStateException(operation + " did not return a list: " + describe(message));
    }

    private static String describe(Message message) {
        if (message == null) {
            return "null response";
        }
        Object data = message.getData();
        return message.getType() + " / " + (data == null ? "null" : data.toString());
    }
}
