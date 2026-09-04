package com.lirasemijoias.projeto.security;

import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void constructorRejectsSecretShorterThanThirtyTwoBytes() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new JwtService("too-short", 60_000));
        assertTrue(exception.getMessage().contains("32"));
    }

    @Test
    void generateCreatesValidSignedTokenWithUserAsSubject() {
        JwtService service = new JwtService(SECRET, 60_000);
        User user = new User();
        user.setEmail("ana@example.com");
        user.setName("Ana");
        user.setRole(UserRole.ADMIN);

        String token = service.generate(user);

        assertTrue(service.isValid(token));
        assertEquals("ana@example.com", service.extractSubject(token));
    }

    @Test
    void isValidRejectsTamperedMalformedNullAndExpiredTokens() throws InterruptedException {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generate(user());

        assertFalse(service.isValid(token.substring(0, token.length() - 1) + "x"));
        assertFalse(service.isValid("not-a-jwt"));
        assertFalse(service.isValid(null));

        JwtService expiring = new JwtService(SECRET, 1);
        String expiringToken = expiring.generate(user());
        Thread.sleep(5);
        assertFalse(expiring.isValid(expiringToken));
    }

    @Test
    void extractSubjectThrowsForTokenSignedWithAnotherKey() {
        JwtService issuer = new JwtService("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 60_000);
        JwtService verifier = new JwtService("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 60_000);
        String token = issuer.generate(user());

        assertThrows(RuntimeException.class, () -> verifier.extractSubject(token));
    }

    private static User user() {
        User user = new User();
        user.setEmail("employee@example.com");
        user.setName("Employee");
        user.setRole(UserRole.EMPLOYEE);
        return user;
    }
}
