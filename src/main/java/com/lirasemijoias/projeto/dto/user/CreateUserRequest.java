package com.lirasemijoias.projeto.dto.user;

import com.lirasemijoias.projeto.model.enums.UserRole;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 8) String password,
        @NotNull UserRole role
) {}
