package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldCreateUserWithRole() {
        var role = new Role();
        role.setDescription("ADMIN");

        var user = new User();
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setActive(true);
        user.getRoles().add(role);

        assertThat(user.getUsername()).isEqualTo("jdoe");
        assertThat(user.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(user.getActive()).isTrue();
        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles().iterator().next().getDescription()).isEqualTo("ADMIN");
    }

    @Test
    void shouldCreateUserWithMultipleRoles() {
        var admin = new Role();
        admin.setDescription("ADMIN");

        var manager = new Role();
        manager.setDescription("MANAGER");

        var user = new User();
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setActive(true);
        user.getRoles().add(admin);
        user.getRoles().add(manager);

        assertThat(user.getRoles()).hasSize(2);
        assertThat(user.getRoles()).extracting(Role::getDescription)
                .containsExactlyInAnyOrder("ADMIN", "MANAGER");
    }
}
