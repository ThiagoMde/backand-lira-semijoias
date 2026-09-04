package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.auth.LoginRequest;
import com.lirasemijoias.projeto.dto.auth.LoginResponse;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.repository.UserRepository;
import com.lirasemijoias.projeto.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        String token = jwtService.generate(user);
        return new LoginResponse(token, user.getName(), user.getEmail(), user.getRole());
    }
}
