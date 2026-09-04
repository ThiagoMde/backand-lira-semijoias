package com.lirasemijoias.projeto.dto.auth;

import com.lirasemijoias.projeto.model.enums.UserRole;

public record LoginResponse(
        String token,
        String name,
        String email,
        UserRole role
) {}
