package com.rocket.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "letters, numbers and _ only")
        String username,

        @NotBlank
        @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
        String password
) {}