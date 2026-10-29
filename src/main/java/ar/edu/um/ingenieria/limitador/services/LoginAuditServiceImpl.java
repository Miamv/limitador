package ar.edu.um.ingenieria.limitador.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.ingenieria.limitador.domain.LoginAudit;
import ar.edu.um.ingenieria.limitador.dto.GeoLocation;
import ar.edu.um.ingenieria.limitador.repository.LoginAuditRepository;

@Service
public class LoginAuditServiceImpl implements LoginAuditService {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditServiceImpl.class);

    private final GeoLocationService geoLocationService;
    private final LoginAuditRepository loginAuditRepository;

    public LoginAuditServiceImpl(GeoLocationService geoLocationService, LoginAuditRepository loginAuditRepository) {
        this.geoLocationService = geoLocationService;
        this.loginAuditRepository = loginAuditRepository;
    }

    @Override
    @Transactional
    public LoginAudit recordLoginAttempt(String username, String ipAddress, boolean success, String failureReason) {
        GeoLocation location = geoLocationService.resolve(ipAddress);
        String user = (username != null && !username.isBlank()) ? username : "desconocido";

        LoginAudit audit = new LoginAudit(
                user,
                location.ip(),
                location.country(),
                location.countryCode(),
                location.city(),
                location.asn(),
                location.asnOrganization(),
                success,
                failureReason
        );

        LoginAudit saved = loginAuditRepository.save(audit);

        if (success) {
            log.info("LOGIN EXITOSO - Usuario: '{}' | IP: '{}' | País: '{}' [{}] | Ciudad: '{}' | ASN: '{}' ({})",
                    user, location.ip(), location.country(), location.countryCode(), location.city(),
                    location.asn(), location.asnOrganization());
        } else {
            log.warn("LOGIN FALLIDO - Usuario: '{}' | IP: '{}' | País: '{}' [{}] | Ciudad: '{}' | Motivo: '{}'",
                    user, location.ip(), location.country(), location.countryCode(), location.city(),
                    failureReason != null ? failureReason : "Credenciales inválidas");
        }

        return saved;
    }
}
