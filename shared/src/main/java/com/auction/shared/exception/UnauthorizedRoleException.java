package com.auction.shared.exception;

/**
 * Exception khi user không có quyền thực hiện hành động.
 */
public class UnauthorizedRoleException extends AuctionException {
    public UnauthorizedRoleException(String action, String requiredRole) {
        super("Không có quyền thực hiện '" + action + "'. Yêu cầu role: " + requiredRole);
    }

    public UnauthorizedRoleException(String message) {
        super(message);
    }
}
