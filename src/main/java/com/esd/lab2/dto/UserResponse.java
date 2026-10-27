package com.esd.lab2.dto;

import com.esd.lab2.entity.Role;
import java.time.Instant;

/**
 * S1 — FILL IN MANUALLY (no AI for this slice).
 *
 * Response DTO for a User. This is returned by signup and GET /api/users/me.
 *
 * Rules:
 *   - Never include passwordHash in this DTO.
 *   - Map directly from the User entity fields.
 */
public record UserResponse(

        // TODO: add the correct fields from the User entity
        //   id       (Long)
        //   email    (String)
        //   fullName (String)
        //   role     (Role)
        //   createdAt (Instant)
        Long id,
        String email,
        String fullName,
        Role role,
        Instant createdAt
) {}
