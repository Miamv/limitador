package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserDetailTest {

    private static User validUser() {
        var user = new User();
        user.setId(1L);
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setPassword("secret");
        user.setActive(true);
        return user;
    }

    // ---------- Happy path ----------

    @Test
    void toDetail_withValidUser_returnsAllDetails() {
        // Arrange
        var user = validUser();
        user.getRoles().add(new Role("ADMIN"));
        user.getRoles().add(new Role("MANAGER"));

        // Act
        UserDetail detail = user.toDetail();

        // Assert
        assertThat(detail.id()).isEqualTo(1L);
        assertThat(detail.username()).isEqualTo("jdoe");
        assertThat(detail.email()).isEqualTo("jdoe@example.com");
        assertThat(detail.active()).isTrue();
        assertThat(detail.roles()).containsExactly("ADMIN", "MANAGER");
    }

    @Test
    void toDetail_withoutRoles_returnsEmptyRoles() {
        // Arrange
        var user = validUser();

        // Act
        UserDetail detail = user.toDetail();

        // Assert
        assertThat(detail.roles()).isEmpty();
    }

    @Test
    void toDetail_shouldNotExposePassword() {
        // Arrange
        var user = validUser();

        // Act
        UserDetail detail = user.toDetail();

        // Assert
        assertThat(detail.toString()).doesNotContain("secret");
    }

    // ---------- Campos nulos / vacíos ----------

    @Test
    void toDetail_withNullActive_treatsUserAsInactive() {
        // Arrange
        var user = validUser();
        user.setActive(null);

        // Act
        UserDetail detail = user.toDetail();

        // Assert
        assertThat(detail.active()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void toDetail_withBlankUsername_throwsIllegalState(String username) {
        // Arrange
        var user = validUser();
        user.setUsername(username);

        // Act + Assert
        assertThatThrownBy(user::toDetail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void toDetail_withBlankEmail_throwsIllegalState(String email) {
        // Arrange
        var user = validUser();
        user.setEmail(email);

        // Act + Assert
        assertThatThrownBy(user::toDetail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email");
    }

    // ---------- Inmutabilidad ----------

    @Test
    void toDetail_returnsImmutableRoles() {
        // Arrange
        var user = validUser();
        user.getRoles().add(new Role("ADMIN"));
        UserDetail detail = user.toDetail();

        // Act + Assert
        assertThatThrownBy(() -> detail.roles().add("HACKER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toDetail_isSnapshot_laterChangesToUserDoNotAffectIt() {
        // Arrange
        var user = validUser();
        user.getRoles().add(new Role("ADMIN"));
        UserDetail detail = user.toDetail();

        // Act
        user.setUsername("changed");
        user.getRoles().add(new Role("MANAGER"));

        // Assert
        assertThat(detail.username()).isEqualTo("jdoe");
        assertThat(detail.roles()).containsExactly("ADMIN");
    }
}
