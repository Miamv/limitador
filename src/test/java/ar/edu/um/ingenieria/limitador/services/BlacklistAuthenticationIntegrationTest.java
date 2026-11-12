package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.LoginAudit;
import ar.edu.um.ingenieria.limitador.domain.LoginStatus;
import ar.edu.um.ingenieria.limitador.domain.Role;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.GeoLocation;
import ar.edu.um.ingenieria.limitador.exceptions.BlacklistedException;
import ar.edu.um.ingenieria.limitador.repository.BlacklistRuleRepository;
import ar.edu.um.ingenieria.limitador.repository.LoginAuditRepository;
import ar.edu.um.ingenieria.limitador.repository.RoleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@DataJpaTest
@Import({
        BlacklistServiceImpl.class,
        LoginAuditServiceImpl.class,
        BlacklistAuthenticationIntegrationTest.IntegrationTestConfig.class
})
class BlacklistAuthenticationIntegrationTest {

    @TestConfiguration
    static class IntegrationTestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        GeoLocationService geoLocationService() {
            return ip -> new GeoLocation(ip, "Argentina", "AR", "Mendoza", 7303L, "Telecom Argentina");
        }
    }

    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private BlacklistRuleRepository blacklistRuleRepository;

    @Autowired
    private LoginAuditService loginAuditService;

    @Autowired
    private LoginAuditRepository loginAuditRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        blacklistRuleRepository.deleteAll();
        loginAuditRepository.deleteAll();
        userRepository.deleteAll();

        // Crear usuario de prueba
        User user = new User();
        user.setUsername("santi");
        user.setEmail("santi@um.edu.ar");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setActive(true);
        user.getRoles().add(roleRepository.save(new Role("USER")));
        userRepository.saveAndFlush(user);
    }

    @Test
    @DisplayName("Flujo exitoso sin reglas de bloqueo en BD")
    void login_whenNotBlacklisted_auditRecordsSuccessAndUserExistsTrue() {
        Optional<BlacklistRule> rule = blacklistService.findMatchingRule("190.112.50.10", "santi");
        assertThat(rule).isEmpty();

        LoginAudit audit = loginAuditService.recordLoginAttempt("santi", "190.112.50.10", LoginStatus.SUCCESS, null);
        assertThat(audit.getId()).isNotNull();
        assertThat(audit.getStatus()).isEqualTo(LoginStatus.SUCCESS);
        assertThat(audit.isSuccess()).isTrue();
        assertThat(audit.isUserExists()).isTrue();
        assertThat(audit.getCity()).isEqualTo("Mendoza");
    }

    @Test
    @DisplayName("Bloqueo por CIDR IPv4 /24 persistido en BD")
    void login_whenIpInIpv4Cidr_isBlockedAndAudited() {
        // Bloquear red 192.168.10.0/24
        blacklistRuleRepository.saveAndFlush(
                BlacklistRule.forIpOrCidr("192.168.10.0/24", "Rango sospechoso"));

        Optional<BlacklistRule> match = blacklistService.findMatchingRule("192.168.10.45", "santi");
        assertThat(match).isPresent();
        assertThat(match.get().getReason()).isEqualTo("Rango sospechoso");

        LoginAudit audit = loginAuditService.recordLoginAttempt("santi", "192.168.10.45", LoginStatus.BLOCKED, match.get().getReason());
        assertThat(audit.getStatus()).isEqualTo(LoginStatus.BLOCKED);
        assertThat(audit.isSuccess()).isFalse();
        assertThat(audit.isUserExists()).isTrue();
        assertThat(audit.getFailureReason()).isEqualTo("Rango sospechoso");
    }

    @Test
    @DisplayName("Bloqueo por CIDR IPv6 persistido en BD")
    void login_whenIpInIpv6Cidr_isBlocked() {
        blacklistRuleRepository.saveAndFlush(
                BlacklistRule.forIpOrCidr("2001:db8:abcd::/48", "Ataque botnet IPv6"));

        Optional<BlacklistRule> match = blacklistService.findMatchingRule("2001:db8:abcd:0012::1", "santi");
        assertThat(match).isPresent();

        Optional<BlacklistRule> nonMatch = blacklistService.findMatchingRule("2001:db8:ff00::1", "santi");
        assertThat(nonMatch).isEmpty();
    }

    @Test
    @DisplayName("Bloqueo por Usuario persistido en BD independientemente de la IP")
    void login_whenUsernameBlacklisted_isBlocked() {
        blacklistRuleRepository.saveAndFlush(
                BlacklistRule.forUsername("santi", "Cuenta congelada preventivamente"));

        Optional<BlacklistRule> match = blacklistService.findMatchingRule("200.1.2.3", "santi");
        assertThat(match).isPresent();
        assertThat(match.get().getReason()).isEqualTo("Cuenta congelada preventivamente");

        // Otro usuario desde la misma IP no queda bloqueado
        Optional<BlacklistRule> otherUser = blacklistService.findMatchingRule("200.1.2.3", "otro");
        assertThat(otherUser).isEmpty();
    }

    @Test
    @DisplayName("Regla expirada en BD deja de bloquear")
    void login_whenRuleExpired_accessAllowed() {
        BlacklistRule rule = BlacklistRule.forUsername("santi", "Bloqueo temporal");
        rule.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        blacklistRuleRepository.saveAndFlush(rule);

        Optional<BlacklistRule> match = blacklistService.findMatchingRule("190.112.50.10", "santi");
        assertThat(match).isEmpty();
    }

    @Test
    @DisplayName("Regla desactivada deja de bloquear")
    void login_whenRuleDeactivated_accessAllowed() {
        BlacklistRule rule = BlacklistRule.forIpOrCidr("190.112.50.10", "Bloqueo puntual");
        rule.setActive(false);
        blacklistRuleRepository.saveAndFlush(rule);

        Optional<BlacklistRule> match = blacklistService.findMatchingRule("190.112.50.10", "santi");
        assertThat(match).isEmpty();
    }

    @Test
    @DisplayName("Usuario inexistente registra userExists = false en auditoría")
    void audit_whenUserDoesNotExist_recordsFlagFalse() {
        LoginAudit audit = loginAuditService.recordLoginAttempt("fantasma", "190.112.50.10", LoginStatus.FAILURE, "User not found");
        assertThat(audit.isUserExists()).isFalse();
    }
}
