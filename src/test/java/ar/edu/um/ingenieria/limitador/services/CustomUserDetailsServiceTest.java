package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final String HASHED_PASSWORD =
            "$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User user(String username, Boolean active) {
        var user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@um.edu.ar");
        user.setPassword(HASHED_PASSWORD);
        user.setActive(active);
        return user;
    }

    @Test
    void loadUserByUsername_withExistingUser_returnsUsernameAndHashedPassword() {
        when(userRepository.findByUsername("jperez")).thenReturn(Optional.of(user("jperez", true)));

        UserDetails details = userDetailsService.loadUserByUsername("jperez");

        assertThat(details.getUsername()).isEqualTo("jperez");
        assertThat(details.getPassword()).isEqualTo(HASHED_PASSWORD);
        assertThat(details.getPassword()).startsWith("$2a$");
    }

    @Test
    void loadUserByUsername_propagatesEnabledStateFromActiveFlag() {
        when(userRepository.findByUsername("activo")).thenReturn(Optional.of(user("activo", true)));
        when(userRepository.findByUsername("inactivo")).thenReturn(Optional.of(user("inactivo", false)));
        when(userRepository.findByUsername("sinEstado")).thenReturn(Optional.of(user("sinEstado", null)));

        assertThat(userDetailsService.loadUserByUsername("activo").isEnabled()).isTrue();
        assertThat(userDetailsService.loadUserByUsername("inactivo").isEnabled()).isFalse();
        assertThat(userDetailsService.loadUserByUsername("sinEstado").isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_withRoles_returnsAuthoritiesMatchingThoseRoles() {
        User user = user("jperez", true);
        user.getRoles().add(new Role("ADMIN"));
        user.getRoles().add(new Role("USER"));
        when(userRepository.findByUsername("jperez")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("jperez");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExist_throwsUsernameNotFound() {
        when(userRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("noexiste"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("noexiste");
    }
}
