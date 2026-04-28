package com.auction.client.MockData;

import java.util.HashMap;

public class DataStore {

    public static HashMap<String, UserSession> users = new HashMap<>();

    public static UserSession currentUser;

    public static String currentLoggedInUser = "";

    static {
        users.put("admin", new  UserSession("admin", "admin", "admin"));
    }
}