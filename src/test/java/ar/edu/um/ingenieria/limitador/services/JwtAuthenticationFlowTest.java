package ar.edu.um.ingenieria.limitador.services;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationFlowTest {

    private static final String TEST_SECRET = "test-only-secret-must-be-at-least-32-bytes!!";
    private static final String OTHER_SECRET = "another-test-secret-also-longer-than-32b!!";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void persistUser(String username, String... roleNames) {
        var user = new User();
        user.setUsername(username);
        user.setEmail(username + "@um.edu.ar");
        user.setPassword(passwordEncoder.encode("Secreto123"));
        user.setActive(true);
        for (String roleName : roleNames) {
            user.getRoles().add(roleRepository.findByDescription(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName))));
        }
        userRepository.saveAndFlush(user);
    }

    private String loginReturningToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(body).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- Login con token ----------

    @Test
    void login_withValidCredentials_returnsJwtAndPublicDataWithoutPassword() throws Exception {
        persistUser("jperez", "USER");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "jperez", "password": "Secreto123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("jperez"))
                .andExpect(jsonPath("$.email").value("jperez@um.edu.ar"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void login_withWrongPassword_stillReturns401() throws Exception {
        persistUser("jperez", "USER");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "jperez", "password": "mala"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    // ---------- Rutas públicas ----------

    @Test
    void register_remainsPublic_andWorksWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "nuevo",
                                  "email": "nuevo@um.edu.ar",
                                  "password": "Secreto123",
                                  "firstName": "Nuevo",
                                  "phoneNumber": "+5492615551234"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo"));
    }

    // ---------- Protección de rutas ----------

    @Test
    void protectedRoute_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withValidToken_returns200() throws Exception {
        persistUser("jperez", "USER");
        String token = loginReturningToken("jperez", "Secreto123");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRoute_withExpiredToken_returns401() throws Exception {
        persistUser("jperez", "USER");
        String expired = expiredTokenSignedWith(TEST_SECRET);

        mockMvc.perform(get("/api/users").header("Authorization", bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withWrongSignatureToken_returns401() throws Exception {
        String forged = new SignedJWTWith(OTHER_SECRET).serializeFor("jperez");

        mockMvc.perform(get("/api/users").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Autoridades ----------

    @Test
    void adminRoute_withUserRoleToken_returns403() throws Exception {
        persistUser("jperez", "ROLE_USER");
        String token = loginReturningToken("jperez", "Secreto123");

        mockMvc.perform(get("/api/roles").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoute_withAdminToken_returns200() throws Exception {
        persistUser("admin", "ROLE_ADMIN");
        String token = loginReturningToken("admin", "Secreto123");

        mockMvc.perform(get("/api/roles").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    // ---------- Tokens firmados a mano (solo para casos negativos) ----------

    private String expiredTokenSignedWith(String secret) {
        return new SignedJWTWith(secret).expiredFor("jperez");
    }

    private static final class SignedJWTWith {
        private final String secret;

        private SignedJWTWith(String secret) {
            this.secret = secret;
        }

        private String serializeFor(String subject) {
            return sign(subject, Date.from(Instant.now().plusSeconds(300)));
        }

        private String expiredFor(String subject) {
            return sign(subject, Date.from(Instant.now().minusSeconds(600)));
        }

        private String sign(String subject, Date expiration) {
            try {
                JWTClaimsSet claims = new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issueTime(Date.from(Instant.now().minusSeconds(660)))
                        .expirationTime(expiration)
                        .claim("roles", List.of("USER"))
                        .build();
                SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
                jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
                return jwt.serialize();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
