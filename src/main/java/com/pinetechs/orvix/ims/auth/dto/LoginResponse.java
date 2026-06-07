package com.pinetechs.orvix.ims.auth.dto;

import com.pinetechs.orvix.ims.user.dto.UserResponse;

public class LoginResponse {
    private String token;
    private UserResponse user;

    public LoginResponse() {}

    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}
