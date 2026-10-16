package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;

import tools.jackson.databind.ObjectMapper;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-only-secret-must-be-at-least-32-bytes!!";
    private static final String OTHER_SECRET = "another-test-secret-also-longer-than-32b!!";
    private static final String HASHED_PASSWORD =
            "$2a$10$abcdefghijklmnopqrstuuXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtService service() {
        return new JwtService(TEST_SECRET, TimeUnit.HOURS.toMillis(1));
    }

    private User user() {
        var user = new User();
        user.setId(1L);
        user.setUsername("jperez");
        user.setEmail("jperez@um.edu.ar");
        user.setPassword(HASHED_PASSWORD);
        user.setActive(true);
        user.getRoles().add(new Role("USER"));
        user.getRoles().add(new Role("ADMIN"));
        return user;
    }

    private Map<String, Object> claimsOf(String token) {
        String payload = token.split("\\.")[1];
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        return MAPPER.readValue(json, Map.class);
    }

    private String expiredTokenSignedWith(String secret) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("jperez")
                .issueTime(Date.from(Instant.now().minusSeconds(660)))
                .expirationTime(Date.from(Instant.now().minusSeconds(600)))
                .claim("roles", List.of("USER"))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    // ---------- Generación ----------

    @Test
    void generateToken_withValidUser_returnsSignedTokenWithIdentityAndRoles() {
        String token = service().generateToken(user());

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(service().extractUsername(token)).isEqualTo("jperez");
        assertThat(service().extractRoles(token)).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void generateToken_claimsContainOnlySubRolesIatAndExp() {
        String token = service().generateToken(user());

        assertThat(claimsOf(token)).containsOnlyKeys("sub", "roles", "iat", "exp");
        assertThat((List<String>) claimsOf(token).get("roles")).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void generateToken_claimsNeverContainPasswordNorHash() {
        String token = service().generateToken(user());

        assertThat(token).doesNotContain("Secreto123").doesNotContain("$2a$10$");
        assertThat(claimsOf(token)).doesNotContainKeys("password", "hashedPassword", "credentials");
    }

    @Test
    void generateToken_keepsRawAuthorityFormat_withoutRolePrefix() {
        String token = service().generateToken(user());

        assertThat(service().extractRoles(token))
                .allSatisfy(role -> assertThat(role).doesNotStartWith("ROLE_"));
    }

    // ---------- Validación: firma, expiración y forma ----------

    @Test
    void token_withExpiredExpiration_isRejected() throws Exception {
        String expired = expiredTokenSignedWith(TEST_SECRET);

        assertThatThrownBy(() -> service().extractUsername(expired))
                .isInstanceOf(JwtValidationException.class);
        assertThat(service().isValid(expired)).isFalse();
    }

    @Test
    void token_signedWithDifferentSecret_isRejected() {
        String foreignToken = new JwtService(OTHER_SECRET, TimeUnit.HOURS.toMillis(1))
                .generateToken(user());

        assertThatThrownBy(() -> service().extractUsername(foreignToken))
                .isInstanceOf(BadJwtException.class)
                .isNotInstanceOf(JwtValidationException.class);
        assertThat(service().isValid(foreignToken)).isFalse();
    }

    @Test
    void token_malformed_isRejected() {
        assertThatThrownBy(() -> service().extractUsername("not-a-jwt"))
                .isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> service().extractUsername("only.one"))
                .isInstanceOf(BadJwtException.class);
        assertThat(service().isValid("not-a-jwt")).isFalse();
    }

    // ---------- Configuración inválida ----------

    @Test
    void constructor_withBlankSecret_throws() {
        assertThatThrownBy(() -> new JwtService("", 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void constructor_withTooShortSecret_throwsWithoutEchoingIt() {
        assertThatThrownBy(() -> new JwtService("short", 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32")
                .hasMessageNotContaining("short");
    }

    @Test
    void constructor_withNonPositiveExpiration_throws() {
        assertThatThrownBy(() -> new JwtService(TEST_SECRET, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(TEST_SECRET, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
