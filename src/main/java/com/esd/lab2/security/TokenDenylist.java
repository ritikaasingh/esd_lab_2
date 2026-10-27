package com.esd.lab2.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TokenDenylist {
    private final ConcurrentMap<String, Instant> revokedTokens =
            new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiry) {
        if (jti == null || jti.isBlank() || expiry == null) {
            throw new IllegalArgumentException("Token ID and expiry are required");
        }
        if (expiry.isAfter(Instant.now())) {
            revokedTokens.put(jti, expiry);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        Instant expiry = revokedTokens.get(jti);
        if (expiry == null) {
            return false;
        }

        if (!expiry.isAfter(Instant.now())) {
            revokedTokens.remove(jti, expiry);
            return false;
        }

        return true;
    }

    public void cleanExpired() {
        Instant now = Instant.now();
        revokedTokens.forEach((jti, expiry) -> {
            if (!expiry.isAfter(now)) {
                revokedTokens.remove(jti, expiry);
            }
        });
    }
}
