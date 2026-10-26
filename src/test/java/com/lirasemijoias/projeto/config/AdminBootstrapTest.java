package com.lirasemijoias.projeto.config;

import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.repository.UserRepository;
import com.lirasemijoias.projeto.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {

    private UserRepository repository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        userService = mock(UserService.class);
    }

    private AdminBootstrap bootstrap(String name, String email, String password) {
        return new AdminBootstrap(repository, userService, name, email, password);
    }

    @Test
    void createsAdminWhenConfiguredAndEmailIsNew() {
        when(repository.existsByEmailIgnoreCase("admin@lira.com")).thenReturn(false);

        bootstrap("Thiago", " admin@lira.com ", "senha-forte").run(null);

        verify(userService).create(new CreateUserRequest("Thiago", " admin@lira.com ", "senha-forte", UserRole.ADMIN));
    }

    @Test
    void usesDefaultNameWhenNameIsBlank() {
        bootstrap(" ", "admin@lira.com", "senha-forte").run(null);

        verify(userService).create(new CreateUserRequest("Administrador", "admin@lira.com", "senha-forte", UserRole.ADMIN));
    }

    @Test
    void skipsWhenEmailAlreadyExists() {
        when(repository.existsByEmailIgnoreCase("admin@lira.com")).thenReturn(true);

        bootstrap("Admin", "admin@lira.com", "senha-forte").run(null);

        verify(userService, never()).create(any());
    }

    @Test
    void skipsWhenEmailOrPasswordIsBlank() {
        bootstrap("Admin", "", "senha-forte").run(null);
        bootstrap("Admin", "admin@lira.com", "").run(null);
        bootstrap("Admin", null, null).run(null);

        verifyNoInteractions(repository, userService);
    }

    @Test
    void skipsWhenPasswordIsTooShort() {
        bootstrap("Admin", "admin@lira.com", "1234567").run(null);

        verifyNoInteractions(repository, userService);
    }
}
