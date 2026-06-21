package com.artgallery.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds the two demo accounts required by the security layer if they do not
 * already exist. Runs for every profile so that both the running application
 * and the integration tests have an {@code admin} and a {@code user}.
 *
 * <p>Passwords are read from environment variables (with development defaults)
 * and stored BCrypt-hashed.</p>
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final JdbcUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.security.user-password:user123}")
    private String userPassword;

    @Override
    public void run(String... args) {
        ensureUser("admin", adminPassword, "ROLE_ADMIN", "ROLE_USER");
        ensureUser("user", userPassword, "ROLE_USER");
    }

    private void ensureUser(String username, String rawPassword, String... roles) {
        if (userDetailsManager.userExists(username)) {
            return;
        }
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        userDetailsManager.createUser(User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .authorities(authorities)
                .build());
        log.info("Seeded account '{}' with roles {}", username, Arrays.toString(roles));
    }
}
