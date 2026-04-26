package com.auction.server.network;

import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.server.service.*;
import com.auction.shared.model.*;
import java.io.*;

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
            switch (message.getType()) {
                case LOGIN_REQUEST:
                    return handleLoginRequest(message.getData());
                case REGISTER_REQUEST:
                    return handleRegisterRequest(message.getData());
                case VIEW_AUCTIONS_REQUEST:
                    return handleViewAuctions();
                case CREATE_AUCTION_REQUEST:
                    return handleCreateAuction(message.getData());
                case PLACE_BID_REQUEST:
                    return handlePlaceBid(message.getData());
                case GET_BIDS_REQUEST:
                    return handleGetBids(message.getData());
                default:
                    return new Message(MessageType.ERROR, "Unknown message type");
            }
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
        java.util.List<Auction> auctions = auctionService.getActiveAuctions();
        return new Message(MessageType.VIEW_AUCTIONS_RESPONSE, auctions);
    }

    private Message handleCreateAuction(Object data) throws Exception {
        Auction auction = (Auction) data;
        boolean success = auctionService.createAuction(auction);

        if (success) {
            return new Message(MessageType.CREATE_AUCTION_RESPONSE, auction);
        } else {
            return new Message(MessageType.CREATE_AUCTION_RESPONSE, "Failed to create auction");
        }
    }

    private Message handlePlaceBid(Object data) throws Exception {
        int[] bidData = (int[]) data;
        int auctionId = bidData[0];
        int bidderId = bidData[1];
        double bidAmount = Double.parseDouble(String.valueOf(bidData[2]));

        boolean success = bidService.placeBid(auctionId, bidderId, bidAmount);

        if (success) {
            return new Message(MessageType.PLACE_BID_RESPONSE, "Bid placed successfully");
        } else {
            return new Message(MessageType.PLACE_BID_RESPONSE, "Failed to place bid");
        }
    }

    private Message handleGetBids(Object data) throws Exception {
        int auctionId = (int) data;
        java.util.List<Bid> bids = bidService.getBidHistory(auctionId);
        return new Message(MessageType.GET_BIDS_RESPONSE, bids);
    }
}