package com.lirasemijoias.projeto.config;

import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.model.enums.UserRole;
import com.lirasemijoias.projeto.repository.UserRepository;
import com.lirasemijoias.projeto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador na inicialização a partir de
 * app.bootstrap-admin.* (ADMIN_NAME, ADMIN_EMAIL, ADMIN_PASSWORD).
 * Não faz nada se as variáveis estiverem vazias ou se o e-mail já existir.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository repository;
    private final UserService userService;
    private final String name;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository repository, UserService userService,
                          @Value("${app.bootstrap-admin.name:Administrador}") String name,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password) {
        this.repository = repository;
        this.userService = userService;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(email) || isBlank(password)) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD não definidos; criação automática do administrador ignorada.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD deve ter ao menos {} caracteres; administrador não criado.", MIN_PASSWORD_LENGTH);
            return;
        }
        if (repository.existsByEmailIgnoreCase(email.trim())) {
            log.info("Administrador {} já existe; nada a fazer.", email.trim());
            return;
        }

        String adminName = isBlank(name) ? "Administrador" : name;
        userService.create(new CreateUserRequest(adminName, email, password, UserRole.ADMIN));
        log.info("Administrador {} criado automaticamente.", email.trim().toLowerCase());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
