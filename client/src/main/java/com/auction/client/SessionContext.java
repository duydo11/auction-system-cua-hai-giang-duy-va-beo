package com.auction.client;

import com.auction.shared.model.user.User;

/** User đã đăng nhập (server); thay cho mock DataStore.currentUser khi dùng socket. */
public final class SessionContext {
    private static User currentUser;

    private SessionContext() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
