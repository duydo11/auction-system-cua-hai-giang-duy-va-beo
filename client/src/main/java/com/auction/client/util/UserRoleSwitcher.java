package com.auction.client.util;

import com.auction.client.SessionContext;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;

/**
 * Utility to switch the in-memory user role between Bidder and Seller.
 *
 * <p>Design decision: the DB stores users with a single role, but the UI lets users
 * toggle between Bidder/Seller views freely. This helper wraps the current user in
 * the requested role subclass, preserving id/username/password/email and balance.</p>
 */
public final class UserRoleSwitcher {

    private UserRoleSwitcher() {}

    /**
     * Wraps the current session user as a {@link Seller} if not already one.
     */
    public static void switchToSellerRole() {
        User u = SessionContext.getCurrentUser();
        if (u == null || u instanceof Seller) {
            return;
        }
        double balance = 0.0;
        if (u instanceof Bidder b) {
            balance = b.getAccountBalance();
        }
        Seller seller = new Seller(u.getId(), u.getUsername(), u.getPassword(), u.getEmail(), 0.0, balance);
        SessionContext.setCurrentUser(seller);
    }

    /**
     * Wraps the current session user as a {@link Bidder} if not already one.
     */
    public static void switchToBidderRole() {
        User u = SessionContext.getCurrentUser();
        if (u == null || u instanceof Bidder) {
            return;
        }
        double balance = 0.0;
        if (u instanceof Seller s) {
            balance = s.getAccountBalance();
        }
        Bidder bidder = new Bidder(u.getId(), u.getUsername(), u.getPassword(), u.getEmail(), balance);
        SessionContext.setCurrentUser(bidder);
    }
}
