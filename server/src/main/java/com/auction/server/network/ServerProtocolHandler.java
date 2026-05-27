package com.auction.server.network;

import com.auction.server.ServiceRegistry;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;
import com.auction.shared.model.auction.AuctionSession;
import com.auction.shared.model.auction.AutoBidConfig;
import com.auction.shared.model.auction.Bid;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.Transaction;
import com.auction.shared.model.user.User;
import com.auction.shared.protocol.Message;
import com.auction.shared.protocol.MessageType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerProtocolHandler {
    private static final Logger logger = Logger.getLogger(ServerProtocolHandler.class.getName());

    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final AutoBidService autoBidService;

    public ServerProtocolHandler() {
        this(ServiceRegistry.USER_SERVICE, ServiceRegistry.AUCTION_SERVICE, ServiceRegistry.BID_SERVICE, ServiceRegistry.AUTO_BID_SERVICE);
    }

    public ServerProtocolHandler(UserService userService, AuctionService auctionService, BidService bidService, AutoBidService autoBidService) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.autoBidService = autoBidService;
    }

    public Message handleMessage(Message message) {
        try {
            Message response = switch (message.getType()) {
                // Auth
                case LOGIN_REQUEST -> handleLoginRequest(message.getData());
                case REGISTER_REQUEST -> handleRegisterRequest(message.getData());
                case LOGOUT_REQUEST -> new Message(MessageType.LOGOUT_RESPONSE, (Object) "OK");

                // Auction
                case VIEW_AUCTIONS_REQUEST -> handleViewAuctions();
                case GET_ALL_AUCTIONS_REQUEST -> handleGetAllAuctions();
                case CREATE_AUCTION_REQUEST -> handleCreateAuction(message.getData());

                // Bidding
                case PLACE_BID_REQUEST -> handlePlaceBid(message.getData());
                case GET_BIDS_REQUEST -> handleGetBids(message.getData());

                // Item Management
                case UPDATE_ITEM_REQUEST -> handleUpdateItem(message.getData());
                case DELETE_ITEM_REQUEST -> handleDeleteItem(message.getData());

                // Auto-Bidding
                case REGISTER_AUTO_BID_REQUEST -> handleRegisterAutoBid(message.getData());
                case CANCEL_AUTO_BID_REQUEST -> handleCancelAutoBid(message.getData());

                // Admin
                case BAN_USER_REQUEST -> handleBanUser(message.getData());
                case GET_ALL_USERS_REQUEST -> handleGetAllUsers();

                // User & Balance
                case GET_USER_INFO_REQUEST -> handleGetUserInfo(message.getData());
                case UPDATE_USER_REQUEST -> handleUpdateUser(message.getData());
                case GET_TRANSACTIONS_REQUEST -> handleGetTransactions(message.getData());
                case SAVE_TRANSACTION_REQUEST -> handleSaveTransaction(message.getData());

                default -> new Message(MessageType.ERROR, "Unknown message type: " + message.getType());
            };
            return tag(message, response);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Lỗi xử lý message " + message.getType(), e);
            return tag(message, new Message(MessageType.ERROR, "Server error: " + e.getMessage()));
        }
    }

    private static Message tag(Message request, Message response) {
        if (request != null && request.getCorrelationId() != null && !request.getCorrelationId().isBlank()) {
            response.setCorrelationId(request.getCorrelationId());
        }
        return response;
    }

    // ========================
    // Auth handlers
    // ========================

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
        String role = userData.length >= 4 ? userData[3] : "BIDDER";
        boolean success = userService.registerUser(userData[0], userData[1], userData[2], role);

        if (success) {
            return new Message(MessageType.REGISTER_RESPONSE, (Object) "Registration successful");
        } else {
            return new Message(MessageType.REGISTER_RESPONSE, "Username already exists");
        }
    }

    // ========================
    // Auction handlers
    // ========================

    private Message handleViewAuctions() throws Exception {
        List<AuctionSession> auctions = auctionService.getActiveAuctions();
        return new Message(MessageType.VIEW_AUCTIONS_RESPONSE, auctions);
    }

    private Message handleGetAllAuctions() throws Exception {
        List<AuctionSession> auctions = auctionService.getAllAuctions();
        return new Message(MessageType.GET_ALL_AUCTIONS_RESPONSE, auctions);
    }

    private Message handleCreateAuction(Object data) throws Exception {
        AuctionSession auction = (AuctionSession) data;
        boolean success = auctionService.createAuction(auction);

        if (success) {
            AuctionSession refreshed = auctionService.getSessionById(auction.getId());
            AuctionSession payload = refreshed != null ? refreshed : auction;
            ClientBroadcastHub.broadcast(new Message(MessageType.AUCTION_CREATED_PUSH, payload));
            return new Message(MessageType.CREATE_AUCTION_RESPONSE, auction);
        } else {
            return new Message(MessageType.CREATE_AUCTION_RESPONSE, "Failed to create auction");
        }
    }

    // ========================
    // Bidding handlers
    // ========================

    private Message handlePlaceBid(Object data) throws Exception {
        int sessionId;
        int bidderId;
        double bidAmount;
        if (data instanceof String[] arr && arr.length >= 3) {
            sessionId = Integer.parseInt(arr[0].trim());
            bidderId = Integer.parseInt(arr[1].trim());
            bidAmount = Double.parseDouble(arr[2].trim());
        } else if (data instanceof int[] arr && arr.length >= 3) {
            sessionId = arr[0];
            bidderId = arr[1];
            bidAmount = Double.parseDouble(String.valueOf(arr[2]));
        } else {
            throw new IllegalArgumentException("PLACE_BID expects String[3] or int[3] payload");
        }

        boolean success = bidService.placeBid(sessionId, bidderId, bidAmount);

        if (success) {
            AuctionSession refreshed = auctionService.getSessionById(sessionId);
            if (refreshed != null) {
                ClientBroadcastHub.broadcast(new Message(MessageType.AUCTION_UPDATED_PUSH, refreshed));
            }
            return new Message(MessageType.PLACE_BID_RESPONSE, (Object) "Bid placed successfully");
        } else {
            return new Message(MessageType.PLACE_BID_RESPONSE, "Bid too low or session closed");
        }
    }

    private Message handleGetBids(Object data) throws Exception {
        int sessionId = Integer.parseInt(String.valueOf(data).trim());
        List<Bid> bids = bidService.getBidHistory(sessionId);
        return new Message(MessageType.GET_BIDS_RESPONSE, bids);
    }

    // ========================
    // Item Management handlers
    // ========================

    private Message handleUpdateItem(Object data) throws Exception {
        Item item = (Item) data;
        boolean success = auctionService.updateItem(item);
        if (success) {
            logger.info("Item updated: " + item.getId());
            return new Message(MessageType.UPDATE_ITEM_RESPONSE, (Object) "Item updated successfully");
        } else {
            return new Message(MessageType.UPDATE_ITEM_RESPONSE, "Failed to update item");
        }
    }

    private Message handleDeleteItem(Object data) throws Exception {
        int itemId = Integer.parseInt(String.valueOf(data).trim());
        boolean success = auctionService.deleteItem(itemId);
        if (success) {
            logger.info("Item deleted: " + itemId);
            return new Message(MessageType.DELETE_ITEM_RESPONSE, (Object) "Item deleted successfully");
        } else {
            return new Message(MessageType.DELETE_ITEM_RESPONSE, "Failed to delete item");
        }
    }

    // ========================
    // Auto-Bidding handlers
    // ========================

    private Message handleRegisterAutoBid(Object data) throws Exception {
        AutoBidConfig config = (AutoBidConfig) data;
        boolean success = autoBidService.registerAutoBid(config);
        if (success) {
            logger.info("Auto-bid registered: bidderId=" + config.getBidderId() + 
                        ", sessionId=" + config.getSessionId() + 
                        ", maxBid=" + config.getMaxBid());
            return new Message(MessageType.REGISTER_AUTO_BID_RESPONSE, (Object) "Auto-bid registered");
        } else {
            return new Message(MessageType.REGISTER_AUTO_BID_RESPONSE, "Failed to register auto-bid");
        }
    }

    private Message handleCancelAutoBid(Object data) throws Exception {
        int[] ids;
        if (data instanceof int[] arr && arr.length >= 2) {
            ids = arr;
        } else if (data instanceof String[] arr && arr.length >= 2) {
            ids = new int[]{Integer.parseInt(arr[0].trim()), Integer.parseInt(arr[1].trim())};
        } else {
            throw new IllegalArgumentException("CANCEL_AUTO_BID expects int[2] or String[2]: [sessionId, bidderId]");
        }
        // TODO: Khi Hải hoàn thành AutoBidService → gọi autoBidService.cancelAutoBid(ids[0], ids[1])
        boolean success = autoBidService.cancelAutoBid(ids[0], ids[1]);
        if (success) {
            logger.info("Auto-bid cancelled: sessionId=" + ids[0] + ", bidderId=" + ids[1]);
            return new Message(MessageType.CANCEL_AUTO_BID_RESPONSE, (Object) "Auto-bid cancelled");
        } else {
            return new Message(MessageType.CANCEL_AUTO_BID_RESPONSE, "Failed to cancel auto-bid");
        }
    }

    // ========================
    // Admin handlers
    // ========================

    private Message handleBanUser(Object data) throws Exception {
        int userId = Integer.parseInt(String.valueOf(data).trim());
        boolean success = userService.banUser(userId);
        if (success) {
            logger.info("User banned: " + userId);
            return new Message(MessageType.BAN_USER_RESPONSE, (Object) "User banned successfully");
        } else {
            return new Message(MessageType.BAN_USER_RESPONSE, "Failed to ban user");
        }
    }

    private Message handleGetAllUsers() throws Exception {
        List<User> users = userService.getAllUsers();
        return new Message(MessageType.GET_ALL_USERS_RESPONSE, users);
    }

    private Message handleGetUserInfo(Object data) throws Exception {
        int userId = Integer.parseInt(String.valueOf(data).trim());
        User user = userService.getUserById(userId);
        if (user != null) {
            return new Message(MessageType.GET_USER_INFO_RESPONSE, user);
        } else {
            return new Message(MessageType.ERROR, "User not found");
        }
    }

    private Message handleUpdateUser(Object data) throws Exception {
        User user = (User) data;
        boolean success = userService.updateUser(user);
        if (success) {
            return new Message(MessageType.UPDATE_USER_RESPONSE, user);
        } else {
            return new Message(MessageType.ERROR, "Failed to update user");
        }
    }

    private Message handleGetTransactions(Object data) throws Exception {
        int userId = Integer.parseInt(String.valueOf(data).trim());
        List<Transaction> list = userService.getTransactionsByUserId(userId);
        return new Message(MessageType.GET_TRANSACTIONS_RESPONSE, list);
    }

    private Message handleSaveTransaction(Object data) throws Exception {
        Transaction transaction = (Transaction) data;
        userService.saveTransaction(transaction);
        return new Message(MessageType.SAVE_TRANSACTION_RESPONSE, transaction);
    }
}

