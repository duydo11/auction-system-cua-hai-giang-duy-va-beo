package com.auction.shared.model.user;

import com.auction.shared.model.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;

    public User(int id, String username, String password, String email) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public abstract String getRoleName();
}