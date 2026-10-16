package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@DataJpaTest
@Import(CustomUserDetailsService.class)
class CustomUserDetailsServiceIntegrationTest {

    private static final String HASHED_PASSWORD =
            "$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User persistUserWithRole(String roleName) {
        Role role = roleRepository.save(new Role(roleName));
        var user = new User();
        user.setUsername("jperez");
        user.setEmail("jperez@um.edu.ar");
        user.setPassword(HASHED_PASSWORD);
        user.setActive(true);
        user.getRoles().add(role);
        return userRepository.saveAndFlush(user);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void loadUserByUsername_afterCommit_recoversRolesWithoutLazyInitializationError() {
        persistUserWithRole("ADMIN");

        UserDetails details = userDetailsService.loadUserByUsername("jperez");

        assertThat(details.getUsername()).isEqualTo("jperez");
        assertThat(details.getPassword()).isEqualTo(HASHED_PASSWORD);
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ADMIN");
        assertThat(((User) details).getRoles())
                .extracting(Role::getDescription)
                .containsExactly("ADMIN");
    }
}
