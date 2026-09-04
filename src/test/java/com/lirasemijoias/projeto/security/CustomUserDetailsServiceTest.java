package com.lirasemijoias.projeto.security;

import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Test
    void loadUserMapsCredentialsRoleAndActiveFlag() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        when(repository.findByEmailIgnoreCase("ADMIN@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(repository)
                .loadUserByUsername("ADMIN@example.com");

        assertAll(
                () -> assertEquals("admin@example.com", details.getUsername()),
                () -> assertEquals("encoded", details.getPassword()),
                () -> assertTrue(details.isEnabled()),
                () -> assertEquals(1, details.getAuthorities().size()),
                () -> assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority())
        );
    }

    @Test
    void loadUserDisablesInactiveAccount() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User();
        user.setEmail("employee@example.com");
        user.setPassword("encoded");
        user.setRole(UserRole.EMPLOYEE);
        user.setActive(false);
        when(repository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(repository).loadUserByUsername(user.getEmail());

        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE")));
    }

    @Test
    void loadUserThrowsForUnknownEmail() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> new CustomUserDetailsService(repository).loadUserByUsername("missing@example.com"));
    }
}
