package com.lirasemijoias.projeto.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutBearerTokenContinuesWithoutJwtWork() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void headerWithAnotherSchemeIsIgnored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void validBearerTokenAuthenticatesLoadedUser() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer signed-token");
        when(jwtService.isValid("signed-token")).thenReturn(true);
        when(jwtService.extractSubject("signed-token")).thenReturn("ana@example.com");
        UserDetails details = new User("ana@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername("ana@example.com")).thenReturn(details);

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertSame(details, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidBearerTokenDoesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtService.isValid("invalid")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractSubject(anyString());
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void validTokenDoesNotReplaceExistingAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");
        when(jwtService.isValid("valid")).thenReturn(true);
        Authentication existing = new UsernamePasswordAuthenticationToken("already", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        filter.doFilterInternal(request, response, chain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractSubject(anyString());
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);
    }
}
