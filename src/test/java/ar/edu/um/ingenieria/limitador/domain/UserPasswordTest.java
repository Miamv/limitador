package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class UserPasswordTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposePasswordAccessor() {
        var user = new User();
        user.setPassword("Secreto123");

        assertThat(user.getPassword()).isEqualTo("Secreto123");
    }

    @Test
    void passwordShouldNotBeExposedInJsonResponses() throws Exception {
        var user = new User();
        user.setId(1L);
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setActive(true);
        user.setPassword("$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        String json = objectMapper.writeValueAsString(user);

        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("Secreto");
        assertThat(objectMapper.readTree(json).has("password")).isFalse();
    }

    @Test
    void passwordShouldBeDeserializableFromJsonRequests() throws Exception {
        String json = "{\"username\":\"nuevo\",\"email\":\"nuevo@um.edu.ar\",\"password\":\"Secreto123\"}";

        User user = objectMapper.readValue(json, User.class);

        assertThat(user.getPassword()).isEqualTo("Secreto123");
    }

    @Test
    void passwordShouldNotLeakThroughToString() {
        var user = new User();
        user.setUsername("jdoe");
        user.setPassword("Secreto123");

        assertThat(user.toString()).doesNotContain("Secreto123");
    }
}
