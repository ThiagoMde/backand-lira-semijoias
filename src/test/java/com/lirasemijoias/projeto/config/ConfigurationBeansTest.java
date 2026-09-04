package com.lirasemijoias.projeto.config;

import com.cloudinary.Cloudinary;
import com.lirasemijoias.projeto.security.CustomUserDetailsService;
import com.lirasemijoias.projeto.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationBeansTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void securityFilterChainDefinesPublicAdminAndFallbackPolicies() throws Exception {
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        SecurityConfig config = new SecurityConfig(jwtFilter, mock(CustomUserDetailsService.class));
        HttpSecurity http = mock(HttpSecurity.class);
        CsrfConfigurer<HttpSecurity> csrf = mock(CsrfConfigurer.class);
        CorsConfigurer<HttpSecurity> cors = mock(CorsConfigurer.class);
        SessionManagementConfigurer<HttpSecurity> sessions = mock(SessionManagementConfigurer.class);
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);

        when(http.csrf(ArgumentMatchers.<Customizer<CsrfConfigurer<HttpSecurity>>>any()))
                .thenAnswer(invocation -> {
                    invocation.<Customizer<CsrfConfigurer<HttpSecurity>>>getArgument(0).customize(csrf);
                    return http;
                });
        when(http.cors(ArgumentMatchers.<Customizer<CorsConfigurer<HttpSecurity>>>any()))
                .thenAnswer(invocation -> {
                    invocation.<Customizer<CorsConfigurer<HttpSecurity>>>getArgument(0).customize(cors);
                    return http;
                });
        when(http.sessionManagement(
                ArgumentMatchers.<Customizer<SessionManagementConfigurer<HttpSecurity>>>any()))
                .thenAnswer(invocation -> {
                    invocation.<Customizer<SessionManagementConfigurer<HttpSecurity>>>getArgument(0)
                            .customize(sessions);
                    return http;
                });
        when(http.authorizeHttpRequests(ArgumentMatchers.<Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
                .AuthorizationManagerRequestMatcherRegistry>>any()))
                .thenAnswer(invocation -> {
                    invocation.<Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
                                    .AuthorizationManagerRequestMatcherRegistry>>getArgument(0)
                            .customize(requests);
                    return http;
                });
        when(requests.requestMatchers(ArgumentMatchers.any(String[].class))).thenReturn(authorizedUrl);
        when(requests.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(requests);
        when(authorizedUrl.authenticated()).thenReturn(requests);
        when(authorizedUrl.denyAll()).thenReturn(requests);
        when(http.authenticationProvider(ArgumentMatchers.any())).thenReturn(http);
        when(http.addFilterBefore(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(http);
        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain actualChain = config.securityFilterChain(http);

        ArgumentCaptor<String[]> matcherGroups = ArgumentCaptor.forClass(String[].class);
        verify(requests, times(2)).requestMatchers(matcherGroups.capture());
        assertAll(
                () -> assertSame(expectedChain, actualChain),
                () -> assertEquals(List.of(
                                "/api/auth/**",
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/orders/checkout",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error"),
                        List.of(matcherGroups.getAllValues().get(0))),
                () -> assertEquals(List.of("/api/admin/**"),
                        List.of(matcherGroups.getAllValues().get(1)))
        );
        verify(csrf).disable();
        verify(sessions).sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        verify(authorizedUrl).permitAll();
        verify(authorizedUrl).authenticated();
        verify(authorizedUrl).denyAll();
        verify(http).authenticationProvider(ArgumentMatchers.any(DaoAuthenticationProvider.class));
        verify(http).addFilterBefore(same(jwtFilter),
                ArgumentMatchers.eq(UsernamePasswordAuthenticationFilter.class));
    }

    @Test
    void cloudinaryBeanUsesConfiguredCredentialsAndSecureUrls() {
        Cloudinary cloudinary = new CloudinaryConfig().cloudinary(
                "lira-cloud", "api-key", "api-secret");

        assertAll(
                () -> assertEquals("lira-cloud", cloudinary.config.cloudName),
                () -> assertEquals("api-key", cloudinary.config.apiKey),
                () -> assertEquals("api-secret", cloudinary.config.apiSecret),
                () -> assertTrue(cloudinary.config.secure)
        );
    }

    @Test
    void corsBeanTrimsOriginsAndAppliesApiPolicyToEveryPath() {
        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(
                "https://loja.exemplo.com, http://localhost:3000");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertAll(
                () -> assertEquals(
                        List.of("https://loja.exemplo.com", "http://localhost:3000"),
                        configuration.getAllowedOrigins()),
                () -> assertEquals(
                        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                        configuration.getAllowedMethods()),
                () -> assertEquals(
                        List.of("Authorization", "Content-Type"),
                        configuration.getAllowedHeaders()),
                () -> assertTrue(configuration.getAllowCredentials())
        );
    }

    @Test
    void mongoTransactionManagerUsesProvidedDatabaseFactory() {
        MongoDatabaseFactory databaseFactory = mock(MongoDatabaseFactory.class);

        MongoTransactionManager manager = new MongoConfig().transactionManager(databaseFactory);

        assertSame(databaseFactory, manager.getDatabaseFactory());
        assertDoesNotThrow(manager::afterPropertiesSet);
    }

    @Test
    void securityPasswordEncoderUsesSaltAndVerifiesOriginalPassword() {
        SecurityConfig config = securityConfig(mock(CustomUserDetailsService.class));
        PasswordEncoder encoder = config.passwordEncoder();

        String firstHash = encoder.encode("senha-segura");
        String secondHash = encoder.encode("senha-segura");

        assertAll(
                () -> assertNotEquals("senha-segura", firstHash),
                () -> assertNotEquals(firstHash, secondHash),
                () -> assertTrue(encoder.matches("senha-segura", firstHash)),
                () -> assertFalse(encoder.matches("senha-incorreta", firstHash))
        );
    }

    @Test
    void authenticationProviderUsesConfiguredUserDetailsAndPasswordEncoder() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        SecurityConfig config = securityConfig(userDetailsService);
        UserDetails details = User.withUsername("admin@exemplo.com")
                .password(config.passwordEncoder().encode("senha-segura"))
                .roles("ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("admin@exemplo.com")).thenReturn(details);
        DaoAuthenticationProvider provider = config.authenticationProvider();

        Authentication authentication = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "admin@exemplo.com", "senha-segura"));

        assertAll(
                () -> assertTrue(authentication.isAuthenticated()),
                () -> assertEquals("admin@exemplo.com", authentication.getName()),
                () -> assertTrue(authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))
        );
        verify(userDetailsService).loadUserByUsername("admin@exemplo.com");
    }

    @Test
    void authenticationManagerBeanDelegatesToSpringConfiguration() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager expected = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expected);

        AuthenticationManager actual = securityConfig(mock(CustomUserDetailsService.class))
                .authenticationManager(authenticationConfiguration);

        assertSame(expected, actual);
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    private static SecurityConfig securityConfig(CustomUserDetailsService userDetailsService) {
        return new SecurityConfig(mock(JwtAuthenticationFilter.class), userDetailsService);
    }
}
