package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.AuthResponse;
import ar.edu.um.ingenieria.limitador.dto.LoginRequest;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@DataJpaTest
@Import({ AuthenticationServiceImpl.class, CustomUserDetailsService.class, JwtService.class,
        AuthenticationIntegrationTest.TestAuthConfig.class })
class AuthenticationIntegrationTest {

    @TestConfiguration
    static class TestAuthConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                PasswordEncoder passwordEncoder) {
            var provider = new DaoAuthenticationProvider(userDetailsService);
            provider.setPasswordEncoder(passwordEncoder);
            return new ProviderManager(provider);
        }
    }

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void persistUser(String username, String plainPassword, boolean active) {
        var user = new User();
        user.setUsername(username);
        user.setEmail(username + "@um.edu.ar");
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setActive(active);
        user.getRoles().add(roleRepository.save(new Role("USER")));
        userRepository.saveAndFlush(user);
    }

    @Test
    void login_withValidCredentialsAndBcryptHash_succeedsWithoutPlainComparison() {
        persistUser("jperez", "Secreto123", true);

        AuthResponse detail = authenticationService.login(new LoginRequest("jperez", "Secreto123"));

        assertThat(detail.token()).isNotEmpty();
        assertThat(detail.username()).isEqualTo("jperez");
        assertThat(detail.active()).isTrue();
        assertThat(detail.roles()).containsExactly("USER");
        assertThat(detail.toString()).doesNotContain("Secreto123");
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        persistUser("jperez", "Secreto123", true);

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("jperez", "otra")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownUser_throwsBadCredentials() {
        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("noexiste", "Secreto123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withInactiveUserAndCorrectPassword_throwsDisabled() {
        persistUser("inactivo", "Secreto123", false);

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("inactivo", "Secreto123")))
                .isInstanceOf(DisabledException.class);
    }
}
