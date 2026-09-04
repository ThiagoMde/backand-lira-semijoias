package com.lirasemijoias.projeto.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUsernameReturnsNullWithoutAuthentication() {
        SecurityContextHolder.clearContext();
        assertNull(SecurityUtils.currentUsername());
    }

    @Test
    void currentUsernameReturnsAuthenticationName() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@example.com", "secret"));

        assertEquals("ana@example.com", SecurityUtils.currentUsername());
    }
}
