package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserDataTest {

    @Test
    void shouldCreateUserDataFromUser() {
        var role = new Role();
        role.setDescription("USER");

        var user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setActive(false);
        user.getRoles().add(role);

        var userData = new UserData();
        userData.setFirstName("Alice");
        userData.setLastName("Smith");
        userData.setAddress("123 Main St");
        userData.setPhoneNumber("555-1234");

        assertThat(userData.getFirstName()).isEqualTo("Alice");
        assertThat(userData.getLastName()).isEqualTo("Smith");
        assertThat(userData.getAddress()).isEqualTo("123 Main St");
        assertThat(userData.getPhoneNumber()).isEqualTo("555-1234");
    }
}
