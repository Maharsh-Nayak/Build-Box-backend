package com.buildbox_backend.dto;
public class AuthResponse {
    public String token;
    public String email;
    public Long id;

    public AuthResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }

    public AuthResponse(String token, String email, Long id) {
        this.token = token;
        this.email = email;
        this.id = id;
    }
}

