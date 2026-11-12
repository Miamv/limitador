package ar.edu.um.ingenieria.limitador.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "login_audits", indexes = {
        @Index(name = "idx_login_audit_username", columnList = "username"),
        @Index(name = "idx_login_audit_ip", columnList = "ipAddress"),
        @Index(name = "idx_login_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_login_audit_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private String ipAddress;

    private String country;

    private String countryCode;

    private String city;

    private Long asn;

    private String asnOrganization;

    @Column(nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoginStatus status = LoginStatus.FAILURE;

    @Column(nullable = false)
    private boolean userExists = false;

    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public LoginAudit(String username, String ipAddress, String country, String countryCode,
                      String city, Long asn, String asnOrganization, LoginStatus status,
                      boolean userExists, String failureReason) {
        this.username = username;
        this.ipAddress = ipAddress;
        this.country = country;
        this.countryCode = countryCode;
        this.city = city;
        this.asn = asn;
        this.asnOrganization = asnOrganization;
        this.status = status;
        this.success = (status == LoginStatus.SUCCESS);
        this.userExists = userExists;
        this.failureReason = failureReason;
        this.timestamp = LocalDateTime.now();
    }

    public LoginAudit(String username, String ipAddress, String country, String countryCode,
                      String city, Long asn, String asnOrganization, boolean success, String failureReason) {
        this(username, ipAddress, country, countryCode, city, asn, asnOrganization,
                success ? LoginStatus.SUCCESS : LoginStatus.FAILURE, false, failureReason);
    }
}
