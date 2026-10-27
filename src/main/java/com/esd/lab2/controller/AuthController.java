package com.esd.lab2.controller;

import com.esd.lab2.dto.LoginRequest;
import com.esd.lab2.dto.LoginResponse;
import com.esd.lab2.dto.SignupRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.security.JwtService;
import com.esd.lab2.security.TokenDenylist;
import com.esd.lab2.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenDenylist tokenDenylist;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            TokenDenylist tokenDenylist) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenDenylist = tokenDenylist;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false)
            String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authorization.substring(7).trim();

        try {
            var claims = jwtService.parseClaims(token);
            String jti = claims.getId();

            if (jti == null || jti.isBlank()) {
                return ResponseEntity.status(401).build();
            }

            tokenDenylist.revoke(jti, claims.getExpiration().toInstant());
            return ResponseEntity.noContent().build();
        } catch (JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(401).build();
        }
    }
}
