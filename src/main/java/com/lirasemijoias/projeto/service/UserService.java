package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(CreateUserRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Email já cadastrado.");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return repository.save(user);
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public User setActive(String id, boolean active) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());
        return repository.save(user);
    }
}
