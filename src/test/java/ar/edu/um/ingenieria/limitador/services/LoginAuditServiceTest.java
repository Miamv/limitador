package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.LoginAudit;
import ar.edu.um.ingenieria.limitador.domain.LoginStatus;
import ar.edu.um.ingenieria.limitador.dto.GeoLocation;
import ar.edu.um.ingenieria.limitador.repository.LoginAuditRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LoginAuditServiceTest {

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private LoginAuditRepository loginAuditRepository;

    @Mock
    private UserRepository userRepository;

    private LoginAuditServiceImpl loginAuditService;

    @BeforeEach
    void setUp() {
        loginAuditService = new LoginAuditServiceImpl(geoLocationService, loginAuditRepository, userRepository);
    }

    @Test
    @DisplayName("Registra intento exitoso de login con datos geolocalizados, ASN y userExists=true")
    void recordLoginAttempt_successfulLogin_persistsAuditCorrectly() {
        GeoLocation loc = new GeoLocation("181.164.20.5", "Argentina", "AR", "Mendoza", 7303L, "Telecom Argentina");
        when(geoLocationService.resolve("181.164.20.5")).thenReturn(loc);
        when(userRepository.existsByUsername("santi")).thenReturn(true);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt("santi", "181.164.20.5", LoginStatus.SUCCESS, null);

        assertThat(audit).isNotNull();
        assertThat(audit.getUsername()).isEqualTo("santi");
        assertThat(audit.getIpAddress()).isEqualTo("181.164.20.5");
        assertThat(audit.getCountry()).isEqualTo("Argentina");
        assertThat(audit.getCountryCode()).isEqualTo("AR");
        assertThat(audit.getCity()).isEqualTo("Mendoza");
        assertThat(audit.getAsn()).isEqualTo(7303L);
        assertThat(audit.getAsnOrganization()).isEqualTo("Telecom Argentina");
        assertThat(audit.getStatus()).isEqualTo(LoginStatus.SUCCESS);
        assertThat(audit.isSuccess()).isTrue();
        assertThat(audit.isUserExists()).isTrue();
        assertThat(audit.getFailureReason()).isNull();
        assertThat(audit.getTimestamp()).isNotNull();

        ArgumentCaptor<LoginAudit> captor = ArgumentCaptor.forClass(LoginAudit.class);
        verify(loginAuditRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("santi");
        assertThat(captor.getValue().isUserExists()).isTrue();
    }

    @Test
    @DisplayName("Registra intento fallido de login guardando motivo y userExists=false si no existe")
    void recordLoginAttempt_failedLogin_persistsFailureReasonAndUserExistsFalse() {
        GeoLocation loc = GeoLocation.unknown("10.0.0.1");
        when(geoLocationService.resolve("10.0.0.1")).thenReturn(loc);
        when(userRepository.existsByUsername("no_existe")).thenReturn(false);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt("no_existe", "10.0.0.1", LoginStatus.FAILURE, "Bad credentials");

        assertThat(audit.getStatus()).isEqualTo(LoginStatus.FAILURE);
        assertThat(audit.isSuccess()).isFalse();
        assertThat(audit.isUserExists()).isFalse();
        assertThat(audit.getFailureReason()).isEqualTo("Bad credentials");
        assertThat(audit.getUsername()).isEqualTo("no_existe");
    }

    @Test
    @DisplayName("Registra intento BLOQUEADO con motivo y geolocalización")
    void recordLoginAttempt_blockedLogin_persistsStatusBlocked() {
        GeoLocation loc = new GeoLocation("203.0.113.1", "Estados Unidos", "US", "Ashburn", 15169L, "Google LLC");
        when(geoLocationService.resolve("203.0.113.1")).thenReturn(loc);
        when(userRepository.existsByUsername("attacker")).thenReturn(true);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt("attacker", "203.0.113.1", LoginStatus.BLOCKED, "IP en lista negra");

        assertThat(audit.getStatus()).isEqualTo(LoginStatus.BLOCKED);
        assertThat(audit.isSuccess()).isFalse();
        assertThat(audit.isUserExists()).isTrue();
        assertThat(audit.getFailureReason()).isEqualTo("IP en lista negra");
    }

    @Test
    @DisplayName("Usuario null o vacío es normalizado como 'desconocido' y userExists=false")
    void recordLoginAttempt_nullUsername_normalizedToDesconocido() {
        GeoLocation loc = GeoLocation.local("127.0.0.1");
        when(geoLocationService.resolve("127.0.0.1")).thenReturn(loc);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt(null, "127.0.0.1", LoginStatus.FAILURE, "No user provided");

        assertThat(audit.getUsername()).isEqualTo("desconocido");
        assertThat(audit.isUserExists()).isFalse();
    }
}
