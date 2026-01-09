package com.buildbox_backend.dto;
public class AuthResponse {
    public String token;
    public String email;

    public AuthResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }
}

