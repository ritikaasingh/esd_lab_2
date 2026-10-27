package com.esd.lab2.security;

import com.esd.lab2.entity.User;
import com.esd.lab2.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenDenylist tokenDenylist;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserRepository userRepository,
        TokenDenylist tokenDenylist
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenDenylist = tokenDenylist;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                var claims = jwtService.parseClaims(token);
                String jti = claims.getId();

                if (jti == null || jti.isBlank()) {
                    throw new IllegalArgumentException("JWT is missing jti");
                }

                if (tokenDenylist.isRevoked(jti)) {
                    SecurityContextHolder.clearContext();
                } else {
                    String email = claims.getSubject();
                    User user = userRepository.findByEmail(email).orElse(null);

                    if (user != null &&
                        SecurityContextHolder.getContext().getAuthentication() == null) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                            ))
                        );
                        SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid or expired tokens remain unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
