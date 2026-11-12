package ar.edu.um.ingenieria.limitador.domain;

import java.time.LocalDateTime;

import ar.edu.um.ingenieria.limitador.util.IpCidrValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blacklist_rules", indexes = {
        @Index(name = "idx_blacklist_username", columnList = "username"),
        @Index(name = "idx_blacklist_ip_cidr", columnList = "ipOrCidr"),
        @Index(name = "idx_blacklist_active", columnList = "active"),
        @Index(name = "idx_blacklist_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
public class BlacklistRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_or_cidr", length = 64)
    private String ipOrCidr;

    @Column(length = 100)
    private String username;

    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime expiresAt;

    public static BlacklistRule forIpOrCidr(String ipOrCidr, String reason) {
        validateIpOrCidrFormat(ipOrCidr);
        BlacklistRule rule = new BlacklistRule();
        rule.setIpOrCidr(ipOrCidr != null ? ipOrCidr.trim() : null);
        rule.setReason(reason);
        return rule;
    }

    public static BlacklistRule forUsername(String username, String reason) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        BlacklistRule rule = new BlacklistRule();
        rule.setUsername(username.trim());
        rule.setReason(reason);
        return rule;
    }

    public static BlacklistRule forBoth(String ipOrCidr, String username, String reason) {
        boolean hasIp = ipOrCidr != null && !ipOrCidr.isBlank();
        boolean hasUser = username != null && !username.isBlank();
        if (!hasIp && !hasUser) {
            throw new IllegalArgumentException("Debe especificar al menos una IP/CIDR o un nombre de usuario");
        }
        if (hasIp) {
            validateIpOrCidrFormat(ipOrCidr);
        }
        BlacklistRule rule = new BlacklistRule();
        rule.setIpOrCidr(hasIp ? ipOrCidr.trim() : null);
        rule.setUsername(hasUser ? username.trim() : null);
        rule.setReason(reason);
        return rule;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isEffective() {
        return active && !isExpired();
    }

    private static void validateIpOrCidrFormat(String ipOrCidr) {
        if (!IpCidrValidator.isValidIpOrCidr(ipOrCidr)) {
            throw new IllegalArgumentException("Formato de IP/CIDR inválido: " + ipOrCidr);
        }
    }
}
