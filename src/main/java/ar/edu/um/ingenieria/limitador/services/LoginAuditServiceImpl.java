package ar.edu.um.ingenieria.limitador.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.ingenieria.limitador.domain.LoginAudit;
import ar.edu.um.ingenieria.limitador.domain.LoginStatus;
import ar.edu.um.ingenieria.limitador.dto.GeoLocation;
import ar.edu.um.ingenieria.limitador.repository.LoginAuditRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@Service
public class LoginAuditServiceImpl implements LoginAuditService {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditServiceImpl.class);

    private final GeoLocationService geoLocationService;
    private final LoginAuditRepository loginAuditRepository;
    private final UserRepository userRepository;

    @Autowired
    public LoginAuditServiceImpl(
            GeoLocationService geoLocationService,
            LoginAuditRepository loginAuditRepository,
            UserRepository userRepository) {
        this.geoLocationService = geoLocationService;
        this.loginAuditRepository = loginAuditRepository;
        this.userRepository = userRepository;
    }

    public LoginAuditServiceImpl(
            GeoLocationService geoLocationService,
            LoginAuditRepository loginAuditRepository) {
        this(geoLocationService, loginAuditRepository, null);
    }

    @Override
    @Transactional
    public LoginAudit recordLoginAttempt(String username, String ipAddress, LoginStatus status, String failureReason) {
        GeoLocation location = geoLocationService.resolve(ipAddress);
        String user = (username != null && !username.isBlank()) ? username.trim() : "desconocido";
        boolean userExists = (userRepository != null && username != null && !username.isBlank())
                && userRepository.existsByUsername(username.trim());

        LoginAudit audit = new LoginAudit(
                user,
                location.ip(),
                location.country(),
                location.countryCode(),
                location.city(),
                location.asn(),
                location.asnOrganization(),
                status,
                userExists,
                failureReason
        );

        LoginAudit saved = loginAuditRepository.save(audit);

        switch (status) {
            case SUCCESS -> log.info("LOGIN EXITOSO - Usuario: '{}' (existe: {}) | IP: '{}' | País: '{}' [{}] | Ciudad: '{}' | ASN: '{}' ({})",
                    user, userExists, location.ip(), location.country(), location.countryCode(), location.city(),
                    location.asn(), location.asnOrganization());
            case BLOCKED -> log.warn("LOGIN BLOQUEADO - Usuario: '{}' (existe: {}) | IP: '{}' | País: '{}' [{}] | Ciudad: '{}' | Motivo: '{}'",
                    user, userExists, location.ip(), location.country(), location.countryCode(), location.city(),
                    failureReason != null ? failureReason : "Bloqueado por lista negra");
            case FAILURE -> log.warn("LOGIN FALLIDO - Usuario: '{}' (existe: {}) | IP: '{}' | País: '{}' [{}] | Ciudad: '{}' | Motivo: '{}'",
                    user, userExists, location.ip(), location.country(), location.countryCode(), location.city(),
                    failureReason != null ? failureReason : "Credenciales inválidas");
        }

        return saved;
    }
}
