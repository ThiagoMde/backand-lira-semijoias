package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.auth.LoginRequest;
import com.lirasemijoias.projeto.dto.auth.LoginResponse;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginReturnsAuthenticationResult() {
        LoginRequest request = new LoginRequest("admin@lira.com", "password");
        LoginResponse login = new LoginResponse(
                "jwt-token", "Admin", "admin@lira.com", UserRole.ADMIN);
        when(service.login(request)).thenReturn(login);

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(login, response.getBody());
        verify(service).login(request);
    }
}
