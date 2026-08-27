package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void shouldSetAndGetDescription() {
        var role = new Role();
        role.setDescription("MANAGER");

        assertThat(role.getDescription()).isEqualTo("MANAGER");
    }

    @Test
    void shouldRoleContainMultipleUsers() {
        var role = new Role();
        role.setDescription("ADMIN");

        var user1 = new User();
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");

        var user2 = new User();
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");

        role.getUsers().add(user1);
        role.getUsers().add(user2);

        assertThat(role.getUsers()).hasSize(2);
        assertThat(role.getUsers()).extracting(User::getUsername)
                .containsExactlyInAnyOrder("alice", "bob");
    }
}
