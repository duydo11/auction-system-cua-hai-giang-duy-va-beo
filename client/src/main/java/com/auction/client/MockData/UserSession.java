package com.auction.client.MockData;

public class UserSession {
    private String username;
    private String email;
    private String password;

    public UserSession(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
}