package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository repository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(repository, passwordEncoder);
    }

    @Test
    void createNormalizesDataEncodesPasswordAndSetsAuditFields() {
        CreateUserRequest request = new CreateUserRequest(
                "  Maria  ", "  MARIA@Example.COM  ", "plain-password", UserRole.EMPLOYEE);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        User result = service.create(request);
        LocalDateTime after = LocalDateTime.now();

        assertAll(
                () -> assertEquals("Maria", result.getName()),
                () -> assertEquals("maria@example.com", result.getEmail()),
                () -> assertEquals("encoded", result.getPassword()),
                () -> assertEquals(UserRole.EMPLOYEE, result.getRole()),
                () -> assertTrue(result.isActive()),
                () -> assertFalse(result.getCreatedAt().isBefore(before)),
                () -> assertFalse(result.getCreatedAt().isAfter(after)),
                () -> assertNotNull(result.getUpdatedAt())
        );
        verify(repository).existsByEmailIgnoreCase("  MARIA@Example.COM  ");
        verify(passwordEncoder).encode("plain-password");
    }

    @Test
    void createRejectsDuplicateEmailBeforeEncoding() {
        when(repository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(
                new CreateUserRequest("Name", "existing@example.com", "password", UserRole.ADMIN)));

        verifyNoInteractions(passwordEncoder);
        verify(repository, never()).save(any());
    }

    @Test
    void findAllDelegatesToRepository() {
        List<User> users = List.of(new User(), new User());
        when(repository.findAll()).thenReturn(users);

        assertSame(users, service.findAll());
    }

    @Test
    void setActiveUpdatesExistingUser() {
        User user = new User();
        user.setActive(true);
        when(repository.findById("u1")).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        User result = service.setActive("u1", false);

        assertSame(user, result);
        assertFalse(user.isActive());
        assertNotNull(user.getUpdatedAt());
        verify(repository).save(user);
    }

    @Test
    void setActiveRejectsUnknownUser() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.setActive("missing", true));
        verify(repository, never()).save(any());
    }
}
