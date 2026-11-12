package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.repository.BlacklistRuleRepository;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private BlacklistRuleRepository blacklistRuleRepository;

    private BlacklistServiceImpl blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new BlacklistServiceImpl(blacklistRuleRepository);
    }

    @Test
    @DisplayName("Bloquea IP individual exacta")
    void isBlacklisted_exactIp_matches() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forIpOrCidr("192.168.1.50", "IP sospechosa");
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert
        assertThat(blacklistService.isBlacklisted("192.168.1.50", "jperez")).isTrue();
        assertThat(blacklistService.isBlacklisted("192.168.1.51", "jperez")).isFalse();
    }

    @Test
    @DisplayName("Bloquea rango CIDR IPv4")
    void isBlacklisted_cidrIpv4_matchesSubnet() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forIpOrCidr("10.0.0.0/24", "Red atacante");
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert
        assertThat(blacklistService.isBlacklisted("10.0.0.1", "cualquiera")).isTrue();
        assertThat(blacklistService.isBlacklisted("10.0.0.254", "cualquiera")).isTrue();
        assertThat(blacklistService.isBlacklisted("10.0.1.1", "cualquiera")).isFalse();
    }

    @Test
    @DisplayName("Bloquea rango CIDR IPv6")
    void isBlacklisted_cidrIpv6_matchesSubnet() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forIpOrCidr("2001:db8::/32", "IPv6 bloqueada");
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert
        assertThat(blacklistService.isBlacklisted("2001:db8:0:0:0:0:0:1", "user1")).isTrue();
        assertThat(blacklistService.isBlacklisted("2001:db8::ffff", "user1")).isTrue();
        assertThat(blacklistService.isBlacklisted("2001:db9::1", "user1")).isFalse();
    }

    @Test
    @DisplayName("Bloquea por usuario independientemente de la IP")
    void isBlacklisted_byUsername_matchesAnyIp() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forUsername("hacker", "Usuario bloqueado");
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert
        assertThat(blacklistService.isBlacklisted("192.168.1.1", "hacker")).isTrue();
        assertThat(blacklistService.isBlacklisted("10.0.0.5", "HACKER")).isTrue(); // case insensitive
        assertThat(blacklistService.isBlacklisted("192.168.1.1", "usuario_bueno")).isFalse();
    }

    @Test
    @DisplayName("Regla combinada con IP y Usuario bloquea si cumple cualquiera de los dos")
    void isBlacklisted_combinedRule_matchesIfEitherSatisfied() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forBoth("192.168.1.100", "badactor", "Regla combinada");
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert: coincide IP aunque no coincida usuario -> bloqueado
        assertThat(blacklistService.isBlacklisted("192.168.1.100", "otro_usuario")).isTrue();

        // coincide usuario aunque no coincida IP -> bloqueado
        assertThat(blacklistService.isBlacklisted("200.50.20.1", "badactor")).isTrue();

        // ninguno coincide -> permitido
        assertThat(blacklistService.isBlacklisted("200.50.20.1", "otro_usuario")).isFalse();
    }

    @Test
    @DisplayName("Regla expirada no bloquea acceso")
    void isBlacklisted_expiredRule_doesNotBlock() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forUsername("temporal", "Bloqueo vencido");
        rule.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert
        assertThat(blacklistService.isBlacklisted("181.164.20.5", "temporal")).isFalse();
    }

    @Test
    @DisplayName("Regla inactiva no bloquea acceso")
    void isBlacklisted_inactiveRule_doesNotBlock() {
        // Arrange
        BlacklistRule rule = BlacklistRule.forIpOrCidr("192.168.1.1", "Desactivada");
        rule.setActive(false);
        when(blacklistRuleRepository.findActiveRulesCandidates(any(), any()))
                .thenReturn(List.of(rule));

        // Act & Assert
        assertThat(blacklistService.isBlacklisted("192.168.1.1", "jperez")).isFalse();
    }

    @Test
    @DisplayName("Permite crear regla válida validando persistencia")
    void createRule_validRule_persistsSuccessfully() {
        BlacklistRule rule = BlacklistRule.forIpOrCidr("192.168.5.0/24", "Bloqueo nuevo");
        when(blacklistRuleRepository.save(any(BlacklistRule.class))).thenReturn(rule);

        BlacklistRule created = blacklistService.createRule(rule);

        assertThat(created).isNotNull();
        assertThat(created.getIpOrCidr()).isEqualTo("192.168.5.0/24");
        verify(blacklistRuleRepository).save(rule);
    }

    @Test
    @DisplayName("Falla al crear regla con formato IP/CIDR inválido")
    void createRule_invalidIp_throwsIllegalArgument() {
        BlacklistRule rule = new BlacklistRule();
        rule.setIpOrCidr("invalid-ip/24");
        rule.setReason("Mal formato");

        assertThatThrownBy(() -> blacklistService.createRule(rule))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Modificación de regla: desactivar regla existente")
    void updateRule_deactivate_updatesState() {
        BlacklistRule existing = BlacklistRule.forUsername("bloqueado", "Inicial");
        existing.setId(10L);
        when(blacklistRuleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(blacklistRuleRepository.save(any(BlacklistRule.class))).thenAnswer(i -> i.getArgument(0));

        existing.setActive(false);
        BlacklistRule updated = blacklistService.updateRule(10L, existing);

        assertThat(updated.isActive()).isFalse();
        verify(blacklistRuleRepository).save(existing);
    }
}
