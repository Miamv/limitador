package ar.edu.um.ingenieria.limitador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlacklistRuleTest {

    @Test
    @DisplayName("Crea regla válida para IP/CIDR")
    void createRule_forIpOrCidr_success() {
        // Arrange & Act
        BlacklistRule rule = BlacklistRule.forIpOrCidr("192.168.1.0/24", "Subnet bloqueada");

        // Assert
        assertThat(rule.getIpOrCidr()).isEqualTo("192.168.1.0/24");
        assertThat(rule.getUsername()).isNull();
        assertThat(rule.getReason()).isEqualTo("Subnet bloqueada");
        assertThat(rule.isActive()).isTrue();
        assertThat(rule.getCreatedAt()).isNotNull();
        assertThat(rule.isEffective()).isTrue();
    }

    @Test
    @DisplayName("Crea regla válida para Usuario")
    void createRule_forUsername_success() {
        // Arrange & Act
        BlacklistRule rule = BlacklistRule.forUsername("hacker", "Cuenta comprometida");

        // Assert
        assertThat(rule.getIpOrCidr()).isNull();
        assertThat(rule.getUsername()).isEqualTo("hacker");
        assertThat(rule.getReason()).isEqualTo("Cuenta comprometida");
        assertThat(rule.isActive()).isTrue();
        assertThat(rule.isEffective()).isTrue();
    }

    @Test
    @DisplayName("Crea regla combinada (IP y Usuario)")
    void createRule_forBoth_success() {
        // Arrange & Act
        BlacklistRule rule = BlacklistRule.forBoth("10.0.0.1", "badactor", "Ataque coordinado");

        // Assert
        assertThat(rule.getIpOrCidr()).isEqualTo("10.0.0.1");
        assertThat(rule.getUsername()).isEqualTo("badactor");
        assertThat(rule.getReason()).isEqualTo("Ataque coordinado");
        assertThat(rule.isEffective()).isTrue();
    }

    @Test
    @DisplayName("Falla al crear regla sin IP ni usuario")
    void createRule_withoutIpAndUsername_throwsException() {
        assertThatThrownBy(() -> BlacklistRule.forBoth(null, null, "Sin destino"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Debe especificar al menos una IP/CIDR o un nombre de usuario");
    }

    @Test
    @DisplayName("Falla al crear regla con IP/CIDR con formato inválido")
    void createRule_withInvalidIpOrCidr_throwsException() {
        assertThatThrownBy(() -> BlacklistRule.forIpOrCidr("999.999.999.999", "Ip rota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de IP/CIDR inválido");
    }

    @Test
    @DisplayName("Regla desactivada no es efectiva")
    void isEffective_whenInactive_returnsFalse() {
        BlacklistRule rule = BlacklistRule.forUsername("user1", "Motivo");
        rule.setActive(false);

        assertThat(rule.isEffective()).isFalse();
    }

    @Test
    @DisplayName("Regla con fecha de expiración pasada no es efectiva")
    void isEffective_whenExpired_returnsFalse() {
        BlacklistRule rule = BlacklistRule.forUsername("user1", "Motivo");
        rule.setExpiresAt(LocalDateTime.now().minusHours(1));

        assertThat(rule.isExpired()).isTrue();
        assertThat(rule.isEffective()).isFalse();
    }

    @Test
    @DisplayName("Regla con fecha de expiración futura es efectiva")
    void isEffective_whenFutureExpiration_returnsTrue() {
        BlacklistRule rule = BlacklistRule.forUsername("user1", "Motivo");
        rule.setExpiresAt(LocalDateTime.now().plusDays(1));

        assertThat(rule.isExpired()).isFalse();
        assertThat(rule.isEffective()).isTrue();
    }
}
