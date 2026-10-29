package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;

import ar.edu.um.ingenieria.limitador.dto.GeoLocation;

class GeoLocationServiceTest {

    @Test
    @DisplayName("Dirección loopback retorna GeoLocation local sin consultar bases de datos")
    void resolve_loopbackAddress_returnsLocalWithoutQueryingDatabases() {
        DatabaseReader cityReader = mock(DatabaseReader.class);
        DatabaseReader countryReader = mock(DatabaseReader.class);
        DatabaseReader asnReader = mock(DatabaseReader.class);

        GeoLocationServiceImpl service = new GeoLocationServiceImpl(cityReader, countryReader, asnReader);

        GeoLocation location = service.resolve("127.0.0.1");

        assertThat(location.ip()).isEqualTo("127.0.0.1");
        assertThat(location.country()).isEqualTo("Local / Red Privada");
        assertThat(location.countryCode()).isEqualTo("LOC");
        assertThat(location.city()).isEqualTo("Localhost");
        assertThat(location.asnOrganization()).isEqualTo("Red Local");

        verifyNoInteractions(cityReader, countryReader, asnReader);
    }

    @Test
    @DisplayName("Dirección privada (192.168.1.100) retorna GeoLocation local")
    void resolve_siteLocalAddress_returnsLocal() {
        DatabaseReader cityReader = mock(DatabaseReader.class);
        DatabaseReader countryReader = mock(DatabaseReader.class);
        DatabaseReader asnReader = mock(DatabaseReader.class);

        GeoLocationServiceImpl service = new GeoLocationServiceImpl(cityReader, countryReader, asnReader);

        GeoLocation location = service.resolve("192.168.1.100");

        assertThat(location.ip()).isEqualTo("192.168.1.100");
        assertThat(location.countryCode()).isEqualTo("LOC");
        verifyNoInteractions(cityReader, countryReader, asnReader);
    }

    @Test
    @DisplayName("IP null o vacía retorna GeoLocation desconocido")
    void resolve_nullOrBlankIp_returnsUnknown() {
        GeoLocationServiceImpl service = new GeoLocationServiceImpl(null, null, null);

        GeoLocation loc1 = service.resolve(null);
        assertThat(loc1.country()).isEqualTo("Desconocido");
        assertThat(loc1.ip()).isEqualTo("0.0.0.0");

        GeoLocation loc2 = service.resolve("   ");
        assertThat(loc2.country()).isEqualTo("Desconocido");
    }

    @Test
    @DisplayName("IP no válida retorna desconocido")
    void resolve_invalidIp_returnsUnknown() {
        GeoLocationServiceImpl service = new GeoLocationServiceImpl(null, null, null);

        GeoLocation loc = service.resolve("invalid-ip-address");
        assertThat(loc.country()).isEqualTo("Desconocido");
    }

    @Test
    @DisplayName("Resolución exitosa utilizando las 3 bases de datos (City, Country fallback y ASN)")
    void resolve_withAllDatabases_combinesData() throws Exception {
        DatabaseReader cityReader = mock(DatabaseReader.class);
        DatabaseReader countryReader = mock(DatabaseReader.class);
        DatabaseReader asnReader = mock(DatabaseReader.class);

        CityResponse cityMock = mock(CityResponse.class);
        Country countryMock = mock(Country.class);
        when(countryMock.getName()).thenReturn("Argentina");
        when(countryMock.getIsoCode()).thenReturn("AR");
        City cMock = mock(City.class);
        when(cMock.getName()).thenReturn("Mendoza");
        when(cityMock.getCountry()).thenReturn(countryMock);
        when(cityMock.getCity()).thenReturn(cMock);

        AsnResponse asnMk = mock(AsnResponse.class);
        when(asnMk.getAutonomousSystemNumber()).thenReturn(7303L);
        when(asnMk.getAutonomousSystemOrganization()).thenReturn("Telecom Argentina");

        when(cityReader.tryCity(any(InetAddress.class))).thenReturn(Optional.of(cityMock));
        when(asnReader.tryAsn(any(InetAddress.class))).thenReturn(Optional.of(asnMk));

        GeoLocationServiceImpl service = new GeoLocationServiceImpl(cityReader, countryReader, asnReader);

        GeoLocation loc = service.resolve("181.164.20.5");

        assertThat(loc.ip()).isEqualTo("181.164.20.5");
        assertThat(loc.country()).isEqualTo("Argentina");
        assertThat(loc.countryCode()).isEqualTo("AR");
        assertThat(loc.city()).isEqualTo("Mendoza");
        assertThat(loc.asn()).isEqualTo(7303L);
        assertThat(loc.asnOrganization()).isEqualTo("Telecom Argentina");
    }

    @Test
    @DisplayName("Fallback a base de datos Country cuando City no contiene el país")
    void resolve_countryFallback_whenCityMissing() throws Exception {
        DatabaseReader cityReader = mock(DatabaseReader.class);
        DatabaseReader countryReader = mock(DatabaseReader.class);
        DatabaseReader asnReader = mock(DatabaseReader.class);

        // City reader devuelve vacío
        when(cityReader.tryCity(any(InetAddress.class))).thenReturn(Optional.empty());

        // Country reader devuelve país
        CountryResponse countryMock = mock(CountryResponse.class);
        Country cMock = mock(Country.class);
        when(cMock.getName()).thenReturn("Chile");
        when(cMock.getIsoCode()).thenReturn("CL");
        when(countryMock.getCountry()).thenReturn(cMock);
        when(countryReader.tryCountry(any(InetAddress.class))).thenReturn(Optional.of(countryMock));

        when(asnReader.tryAsn(any(InetAddress.class))).thenReturn(Optional.empty());

        GeoLocationServiceImpl service = new GeoLocationServiceImpl(cityReader, countryReader, asnReader);

        GeoLocation loc = service.resolve("200.72.1.1");

        assertThat(loc.country()).isEqualTo("Chile");
        assertThat(loc.countryCode()).isEqualTo("CL");
        assertThat(loc.city()).isEqualTo("Desconocido");
    }

    @Test
    @DisplayName("Integración real con los archivos GeoLite2 en classpath (City, Country, ASN)")
    void resolve_realClasspathDatabases_resolvesPublicIp() {
        DefaultResourceLoader loader = new DefaultResourceLoader();
        GeoLocationServiceImpl service = new GeoLocationServiceImpl(
                loader,
                "classpath:geolite/GeoLite2-City.mmdb",
                "classpath:geolite/GeoLite2-Country.mmdb",
                "classpath:geolite/GeoLite2-ASN.mmdb"
        );
        service.init();

        try {
            // IP pública reconocida: Google DNS (8.8.8.8)
            GeoLocation location = service.resolve("8.8.8.8");

            assertThat(location).isNotNull();
            assertThat(location.ip()).isEqualTo("8.8.8.8");
            assertThat(location.countryCode()).isEqualTo("US");
            assertThat(location.country()).isEqualTo("United States");
            assertThat(location.asn()).isEqualTo(15169L);
            assertThat(location.asnOrganization()).contains("Google");
        } finally {
            service.close();
        }
    }
}
