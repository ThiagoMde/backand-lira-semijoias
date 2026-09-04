package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.auth.LoginRequest;
import com.lirasemijoias.projeto.dto.auth.LoginResponse;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.repository.UserRepository;
import com.lirasemijoias.projeto.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        service = new AuthService(authenticationManager, userRepository, jwtService);
    }

    @Test
    void loginAuthenticatesAndBuildsResponseFromPersistedUser() {
        LoginRequest request = new LoginRequest("ADMIN@EXAMPLE.COM", "secret");
        Authentication authentication = mock(Authentication.class);
        User user = user("Ana", "admin@example.com", UserRole.ADMIN);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("signed-token");

        LoginResponse response = service.login(request);

        assertAll(
                () -> assertEquals("signed-token", response.token()),
                () -> assertEquals("Ana", response.name()),
                () -> assertEquals("admin@example.com", response.email()),
                () -> assertEquals(UserRole.ADMIN, response.role())
        );
        var captor = org.mockito.ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, captor.getValue());
        assertEquals("ADMIN@EXAMPLE.COM", captor.getValue().getPrincipal());
        assertEquals("secret", captor.getValue().getCredentials());
        verify(jwtService).generate(user);
    }

    @Test
    void loginPropagatesAuthenticationFailureWithoutLookingUpUser() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("x@example.com", "wrong")));

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void loginFailsWhenAuthenticatedUserNoLongerExists() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("gone@example.com");
        when(userRepository.findByEmailIgnoreCase("gone@example.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.login(new LoginRequest("gone@example.com", "secret")));
        verifyNoInteractions(jwtService);
    }

    private static User user(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
