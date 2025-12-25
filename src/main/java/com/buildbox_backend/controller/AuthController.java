package com.buildbox_backend.controller;


import com.buildbox_backend.dto.AuthResponse;
import com.buildbox_backend.dto.LoginRequest;
import com.buildbox_backend.dto.SignupRequest;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.UserRepository;
import com.buildbox_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    // ---------- SIGNUP ----------
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        User user = authService.signup(request.name, request.email, request.password);

        String token = authService.login(request.email, request.password);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    // ---------- LOGIN ----------
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.email, request.password);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    // ---------- CURRENT USER ----------
    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }
}

