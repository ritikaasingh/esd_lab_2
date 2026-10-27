package com.esd.lab2.controller;

import com.esd.lab2.dto.UpdateProfileRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.entity.User;
import com.esd.lab2.exception.ResourceNotFoundException;
import com.esd.lab2.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(toResponse(currentUser(authentication)));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUser(authentication);
        user.setFullName(request.fullName().trim());
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + authentication.getName()));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
