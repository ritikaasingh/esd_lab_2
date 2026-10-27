
package com.esd.lab2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/auth/signup.
 */
public record SignupRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = ".*[0-9].*",
                 message = "Password must contain at least one digit")
        String password,

        @NotBlank
        @Size(max = 100)
        String fullName
) {}
