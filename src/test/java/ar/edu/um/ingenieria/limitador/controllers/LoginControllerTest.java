package ar.edu.um.ingenieria.limitador.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.LoginStatus;
import ar.edu.um.ingenieria.limitador.dto.AuthResponse;
import ar.edu.um.ingenieria.limitador.dto.LoginRequest;
import ar.edu.um.ingenieria.limitador.exceptions.GlobalExceptionHandler;
import ar.edu.um.ingenieria.limitador.services.AuthenticationService;
import ar.edu.um.ingenieria.limitador.services.BlacklistService;
import ar.edu.um.ingenieria.limitador.services.LoginAuditService;
import ar.edu.um.ingenieria.limitador.util.ClientIpResolver;

@WebMvcTest(LoginController.class)
@Import(GlobalExceptionHandler.class)
class LoginControllerTest {

    private static final String TEST_JWT = "header.payload.signature";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private LoginAuditService loginAuditService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private BlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        when(clientIpResolver.extractClientIp(any())).thenReturn("190.112.50.10");
        when(blacklistService.findMatchingRule(any(), any())).thenReturn(Optional.empty());
    }

    private AuthResponse authResponse() {
        return new AuthResponse(TEST_JWT, 1L, "jperez", "jperez@um.edu.ar", true,
                java.util.Set.of("USER"));
    }

    @Test
    void login_withValidCredentials_returns200WithoutPassword_andAuditsSuccess() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", "190.112.50.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "password": "Secreto123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(TEST_JWT))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("jperez"))
                .andExpect(jsonPath("$.email").value("jperez@um.edu.ar"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(loginAuditService).recordLoginAttempt(eq("jperez"), eq("190.112.50.10"), eq(LoginStatus.SUCCESS), isNull());
    }

    @Test
    void login_responseJson_shouldNeverContainPasswordNorHash() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(authResponse());

        String json = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "password": "Secreto123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(json)
                .doesNotContain("password")
                .doesNotContain("$2a$10$")
                .doesNotContain("Secreto123");
    }

    @Test
    void login_withWrongPasswordOrUnknownUser_returns401ProblemDetail_andAuditsFailure() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "password": "mala"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid credentials"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(loginAuditService).recordLoginAttempt(eq("jperez"), eq("190.112.50.10"), eq(LoginStatus.FAILURE), eq("Bad credentials"));
    }

    @Test
    void login_withInactiveUser_returns403ProblemDetail_andAuditsFailure() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new DisabledException("User is disabled"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "inactivo",
                                  "password": "Secreto123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("User is disabled"));

        verify(loginAuditService).recordLoginAttempt(eq("inactivo"), eq("190.112.50.10"), eq(LoginStatus.FAILURE), eq("User is disabled"));
    }

    @Test
    void login_withBlankFields_returns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors.username").value("username is required"))
                .andExpect(jsonPath("$.errors.password").value("password is required"));

        verify(authenticationService, never()).login(any());
        verify(loginAuditService, never()).recordLoginAttempt(any(), any(), any(LoginStatus.class), any());
    }

    @Test
    @DisplayName("Cuando la IP está en blacklist, responde 403, registra intento bloqueado y NUNCA emite JWT ni llama a autenticación")
    void login_whenIpBlacklisted_returns403_andAuditsBlocked_andNeverIssuesJwt() throws Exception {
        BlacklistRule rule = BlacklistRule.forIpOrCidr("190.112.50.10", "Dirección IP maliciosa");
        when(blacklistService.findMatchingRule("190.112.50.10", "jperez")).thenReturn(Optional.of(rule));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "password": "Secreto123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Dirección IP maliciosa"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(loginAuditService).recordLoginAttempt(eq("jperez"), eq("190.112.50.10"), eq(LoginStatus.BLOCKED), eq("Dirección IP maliciosa"));
        verify(authenticationService, never()).login(any());
    }

    @Test
    @DisplayName("Cuando el Usuario está en blacklist, responde 403, registra intento bloqueado y NUNCA emite JWT")
    void login_whenUserBlacklisted_returns403_andAuditsBlocked() throws Exception {
        BlacklistRule rule = BlacklistRule.forUsername("atacante", "Cuenta suspendida por seguridad");
        when(blacklistService.findMatchingRule("190.112.50.10", "atacante")).thenReturn(Optional.of(rule));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "atacante",
                                  "password": "CualquierPassword"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Cuenta suspendida por seguridad"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(loginAuditService).recordLoginAttempt(eq("atacante"), eq("190.112.50.10"), eq(LoginStatus.BLOCKED), eq("Cuenta suspendida por seguridad"));
        verify(authenticationService, never()).login(any());
    }
}
