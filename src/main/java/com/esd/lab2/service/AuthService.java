
package com.esd.lab2.service;

import com.esd.lab2.dto.LoginRequest;
import com.esd.lab2.dto.LoginResponse;
import com.esd.lab2.dto.SignupRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.entity.Role;
import com.esd.lab2.entity.User;
import com.esd.lab2.exception.DuplicateResourceException;
import com.esd.lab2.exception.InvalidCredentialsException;
import com.esd.lab2.repository.UserRepository;
import com.esd.lab2.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse signup(SignupRequest request) {

        // 1. Normalize email
        String email = request.email().trim().toLowerCase();

        // 2. Reject duplicate email addresses
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                "Email already registered"
            );
        }

        // 3. Hash the password
        String hashedPassword =
            passwordEncoder.encode(request.password());

        // 4. Create the user with the STUDENT role
        User user = new User(
            email,
            hashedPassword,
            request.fullName().trim(),
            Role.STUDENT
        );

        // 5. Save the user to the database
        User savedUser = userRepository.save(user);

        // 6. Return a safe response DTO
        return toResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {

        // 1. Normalize the email
        String email = request.email().trim().toLowerCase();

        // 2. Find the user
        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new InvalidCredentialsException(
                    "Invalid email or password"
                )
            );

        // 3. Verify the password against the stored BCrypt hash
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {
            throw new InvalidCredentialsException(
                "Invalid email or password"
            );
        }

        // 4. Generate a signed JWT
        String token = jwtService.generateToken(user);

        // 5. Return the token and its expiry duration
        return new LoginResponse(
            token,
            "Bearer",
            jwtService.getExpirationSeconds()
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
