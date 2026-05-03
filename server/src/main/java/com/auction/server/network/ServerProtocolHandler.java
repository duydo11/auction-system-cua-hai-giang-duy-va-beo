package com.auction.server.network;

import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.util.List;

public class ServerProtocolHandler {
    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public ServerProtocolHandler() {
        this.userService = new UserService();
        this.auctionService = new AuctionService();
        this.bidService = new BidService();
    }

    public Message handleMessage(Message message) {
        try {
            return switch (message.getType()) {
                case LOGIN_REQUEST -> handleLoginRequest(message.getData());
                case REGISTER_REQUEST -> handleRegisterRequest(message.getData());
                case VIEW_AUCTIONS_REQUEST -> handleViewAuctions();
                case CREATE_AUCTION_REQUEST -> handleCreateAuction(message.getData());
                case PLACE_BID_REQUEST -> handlePlaceBid(message.getData());
                case GET_BIDS_REQUEST -> handleGetBids(message.getData());
                default -> new Message(MessageType.ERROR, "Unknown message type");
            };
        } catch (Exception e) {
            return new Message(MessageType.ERROR, "Server error: " + e.getMessage());
        }
    }

    private Message handleLoginRequest(Object data) throws Exception {
        String[] credentials = (String[]) data;
        String username = credentials[0];
        String password = credentials[1];

        User user = userService.loginUser(username, password);
        if (user != null) {
            return new Message(MessageType.LOGIN_RESPONSE, user);
        } else {
            return new Message(MessageType.LOGIN_RESPONSE, "Invalid username or password");
        }
    }

    private Message handleRegisterRequest(Object data) throws Exception {
        String[] userData = (String[]) data;
        boolean success = userService.registerUser(userData[0], userData[1], userData[2], userData[3]);

        if (success) {
            return new Message(MessageType.REGISTER_RESPONSE, "Registration successful");
        } else {
            return new Message(MessageType.REGISTER_RESPONSE, "Username already exists");
        }
    }

    private Message handleViewAuctions() throws Exception {
        List<AuctionSession> auctions = auctionService.getActiveAuctions();
        return new Message(MessageType.VIEW_AUCTIONS_RESPONSE, auctions);
    }

    private Message handleCreateAuction(Object data) throws Exception {
        AuctionSession auction = (AuctionSession) data;
        boolean success = auctionService.createAuction(auction);

        if (success) {
            return new Message(MessageType.CREATE_AUCTION_RESPONSE, auction);
        } else {
            return new Message(MessageType.CREATE_AUCTION_RESPONSE, "Failed to create auction");
        }
    }

    private Message handlePlaceBid(Object data) throws Exception {
        String sessionId;
        String bidderId;
        double bidAmount;
        if (data instanceof String[] arr && arr.length >= 3) {
            sessionId = arr[0];
            bidderId = arr[1];
            bidAmount = Double.parseDouble(arr[2]);
        } else if (data instanceof int[] arr && arr.length >= 3) {
            sessionId = String.valueOf(arr[0]);
            bidderId = String.valueOf(arr[1]);
            bidAmount = Double.parseDouble(String.valueOf(arr[2]));
        } else {
            throw new IllegalArgumentException("PLACE_BID expects String[3] or int[3] payload");
        }

        boolean success = bidService.placeBid(sessionId, bidderId, bidAmount);

        if (success) {
            return new Message(MessageType.PLACE_BID_RESPONSE, "Bid placed successfully");
        } else {
            return new Message(MessageType.PLACE_BID_RESPONSE, "Failed to place bid");
        }
    }

    private Message handleGetBids(Object data) throws Exception {
        String sessionId = data instanceof String ? (String) data : String.valueOf(data);
        List<Bid> bids = bidService.getBidHistory(sessionId);
        return new Message(MessageType.GET_BIDS_RESPONSE, bids);
    }
}
