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
import ar.edu.um.ingenieria.limitador.dto.GeoLocation;
import ar.edu.um.ingenieria.limitador.repository.LoginAuditRepository;

@ExtendWith(MockitoExtension.class)
class LoginAuditServiceTest {

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private LoginAuditRepository loginAuditRepository;

    private LoginAuditServiceImpl loginAuditService;

    @BeforeEach
    void setUp() {
        loginAuditService = new LoginAuditServiceImpl(geoLocationService, loginAuditRepository);
    }

    @Test
    @DisplayName("Registra intento exitoso de login con datos geolocalizados y ASN")
    void recordLoginAttempt_successfulLogin_persistsAuditCorrectly() {
        GeoLocation loc = new GeoLocation("181.164.20.5", "Argentina", "AR", "Mendoza", 7303L, "Telecom Argentina");
        when(geoLocationService.resolve("181.164.20.5")).thenReturn(loc);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt("santi", "181.164.20.5", true, null);

        assertThat(audit).isNotNull();
        assertThat(audit.getUsername()).isEqualTo("santi");
        assertThat(audit.getIpAddress()).isEqualTo("181.164.20.5");
        assertThat(audit.getCountry()).isEqualTo("Argentina");
        assertThat(audit.getCountryCode()).isEqualTo("AR");
        assertThat(audit.getCity()).isEqualTo("Mendoza");
        assertThat(audit.getAsn()).isEqualTo(7303L);
        assertThat(audit.getAsnOrganization()).isEqualTo("Telecom Argentina");
        assertThat(audit.isSuccess()).isTrue();
        assertThat(audit.getFailureReason()).isNull();
        assertThat(audit.getTimestamp()).isNotNull();

        ArgumentCaptor<LoginAudit> captor = ArgumentCaptor.forClass(LoginAudit.class);
        verify(loginAuditRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("santi");
    }

    @Test
    @DisplayName("Registra intento fallido de login guardando el motivo de error")
    void recordLoginAttempt_failedLogin_persistsFailureReason() {
        GeoLocation loc = GeoLocation.unknown("10.0.0.1");
        when(geoLocationService.resolve("10.0.0.1")).thenReturn(loc);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt("admin", "10.0.0.1", false, "Bad credentials");

        assertThat(audit.isSuccess()).isFalse();
        assertThat(audit.getFailureReason()).isEqualTo("Bad credentials");
        assertThat(audit.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Usuario null o vacío es normalizado como 'desconocido'")
    void recordLoginAttempt_nullUsername_normalizedToDesconocido() {
        GeoLocation loc = GeoLocation.local("127.0.0.1");
        when(geoLocationService.resolve("127.0.0.1")).thenReturn(loc);
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginAudit audit = loginAuditService.recordLoginAttempt(null, "127.0.0.1", false, "No user provided");

        assertThat(audit.getUsername()).isEqualTo("desconocido");
    }
}
