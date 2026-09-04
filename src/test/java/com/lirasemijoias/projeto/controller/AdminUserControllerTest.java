package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService service;

    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminUserController(service);
    }

    @Test
    void findAllRemovesPasswordsFromEveryUser() {
        User first = userWithPassword("secret-one");
        User second = userWithPassword("secret-two");
        List<User> users = List.of(first, second);
        when(service.findAll()).thenReturn(users);

        ResponseEntity<List<User>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(users, response.getBody());
        assertNull(first.getPassword());
        assertNull(second.getPassword());
        verify(service).findAll();
    }

    @Test
    void createRemovesPasswordAndReturnsCreatedUser() {
        CreateUserRequest request = new CreateUserRequest(
                "Admin", "admin@lira.com", "password", UserRole.ADMIN);
        User user = userWithPassword("encoded-password");
        when(service.create(request)).thenReturn(user);

        ResponseEntity<User> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(user, response.getBody());
        assertNull(user.getPassword());
        verify(service).create(request);
    }

    @Test
    void statusRemovesPasswordAndReturnsUpdatedUser() {
        User user = userWithPassword("encoded-password");
        when(service.setActive("user-id", false)).thenReturn(user);

        ResponseEntity<User> response = controller.status("user-id", false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(user, response.getBody());
        assertNull(user.getPassword());
        verify(service).setActive("user-id", false);
    }

    private User userWithPassword(String password) {
        User user = new User();
        user.setPassword(password);
        return user;
    }
}
