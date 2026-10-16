package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

class UserDetailsContractTest {

    private static User user() {
        var user = new User();
        user.setId(1L);
        user.setUsername("jdoe");
        user.setEmail("jdoe@example.com");
        user.setPassword("$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        user.setActive(true);
        return user;
    }

    // ---------- Integración con la interfaz ----------

    @Test
    void user_isSpringSecurityUserDetails() {
        assertThat(user()).isInstanceOf(UserDetails.class);
    }

    // ---------- Identidad ----------

    @Test
    void userDetails_returnsUsernameAndStoredPassword() {
        UserDetails details = (UserDetails) user();

        assertThat(details.getUsername()).isEqualTo("jdoe");
        assertThat(details.getPassword())
                .isEqualTo("$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    // ---------- Autoridades desde roles ----------

    @Test
    void userDetails_withRoles_exposesThemAsGrantedAuthorities() {
        User user = user();
        user.getRoles().add(new Role("ADMIN"));
        user.getRoles().add(new Role("USER"));

        UserDetails details = (UserDetails) user;

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void userDetails_withoutRoles_returnsEmptyAuthorities() {
        UserDetails details = (UserDetails) user();

        assertThat(details.getAuthorities()).isEmpty();
    }

    // ---------- isEnabled() según el modelo actual (Boolean.TRUE.equals(active)) ----------

    @Test
    void isEnabled_whenActiveTrue_isEnabled() {
        User user = user();
        user.setActive(true);

        assertThat(((UserDetails) user).isEnabled()).isTrue();
    }

    @Test
    void isEnabled_whenActiveFalse_isNotEnabled() {
        User user = user();
        user.setActive(false);

        assertThat(((UserDetails) user).isEnabled()).isFalse();
    }

    @Test
    void isEnabled_whenActiveNull_isNotEnabled() {
        User user = user();
        user.setActive(null);

        assertThat(((UserDetails) user).isEnabled()).isFalse();
        // consistente con la semántica del modelo existente (User.toDetail)
        assertThat(user.toDetail().active()).isFalse();
    }

    // ---------- Estados no definidos en el modelo: sin atributos nuevos ----------

    @Test
    void userDetails_withoutExtraAccountFlags_reportsDefaultTrueState() {
        UserDetails details = (UserDetails) user();

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    // ---------- Serialización: los estados de seguridad no se exponen en JSON ----------

    @Test
    void userDetails_securityPropertiesAreNotExposedInJson() throws Exception {
        User user = user();
        user.getRoles().add(new Role("ADMIN"));

        String json = new tools.jackson.databind.ObjectMapper().writeValueAsString(user);

        assertThat(json).doesNotContain("authorities")
                .doesNotContain("accountNonExpired")
                .doesNotContain("accountNonLocked")
                .doesNotContain("credentialsNonExpired")
                .doesNotContain("\"enabled\"");
    }
}
