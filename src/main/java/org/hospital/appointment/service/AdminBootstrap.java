package org.hospital.appointment.service;

import java.util.Locale;
import java.nio.charset.StandardCharsets;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Set both BOOTSTRAP_ADMIN_EMAIL and a 12-72 character BOOTSTRAP_ADMIN_PASSWORD");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            userRepository.save(new AppUser(normalizedEmail, passwordEncoder.encode(password), UserRole.ADMIN));
        }
    }
}
