package com.buildbox_backend.controller;

import com.buildbox_backend.dto.AuthResponse;
import com.buildbox_backend.dto.LoginRequest;
import com.buildbox_backend.dto.SignupRequest;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.UserRepository;
import com.buildbox_backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    // ---------- SIGNUP ----------
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {

        System.out.println("Signup request: " + request);

        try {
            User user = authService.signup(request.name, request.email, request.password);

            String token = authService.login(request.email, request.password);
            return ResponseEntity.ok(new AuthResponse(token, request.email, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // ---------- LOGIN ----------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        System.out.println("Login request: " + request.email + ", " + request.password);
        try {
            String token = authService.login(request.email, request.password);
            User user = userRepository.findByEmail(request.email).orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(new AuthResponse(token, request.email, user.getId()));
        } catch (RuntimeException e) {
            if ("User not found".equals(e.getMessage())) {
                return ResponseEntity.status(404).body(e.getMessage());
            } else if ("Invalid credentials".equals(e.getMessage())) {
                return ResponseEntity.status(401).body(e.getMessage());
            }
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // ---------- CURRENT USER ----------
    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).build();

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/oauth/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/oauth/github")
    public void githubLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/github");
    }

}
