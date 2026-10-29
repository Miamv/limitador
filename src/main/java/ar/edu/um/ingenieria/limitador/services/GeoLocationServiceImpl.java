package ar.edu.um.ingenieria.limitador.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;

import ar.edu.um.ingenieria.limitador.dto.GeoLocation;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationServiceImpl.class);

    private final ResourceLoader resourceLoader;
    private final String cityDatabasePath;
    private final String countryDatabasePath;
    private final String asnDatabasePath;

    private DatabaseReader cityReader;
    private DatabaseReader countryReader;
    private DatabaseReader asnReader;

    @Autowired
    public GeoLocationServiceImpl(
            ResourceLoader resourceLoader,
            @Value("${geolite.city-database-path:classpath:geolite/GeoLite2-City.mmdb}") String cityDatabasePath,
            @Value("${geolite.country-database-path:classpath:geolite/GeoLite2-Country.mmdb}") String countryDatabasePath,
            @Value("${geolite.asn-database-path:classpath:geolite/GeoLite2-ASN.mmdb}") String asnDatabasePath) {
        this.resourceLoader = resourceLoader;
        this.cityDatabasePath = cityDatabasePath;
        this.countryDatabasePath = countryDatabasePath;
        this.asnDatabasePath = asnDatabasePath;
    }

    // Constructor para pruebas unitarias o inyección directa de los readers
    public GeoLocationServiceImpl(
            DatabaseReader cityReader,
            DatabaseReader countryReader,
            DatabaseReader asnReader) {
        this.resourceLoader = null;
        this.cityDatabasePath = null;
        this.countryDatabasePath = null;
        this.asnDatabasePath = null;
        this.cityReader = cityReader;
        this.countryReader = countryReader;
        this.asnReader = asnReader;
    }

    @PostConstruct
    public void init() {
        if (resourceLoader == null) {
            return;
        }
        this.cityReader = loadReader(cityDatabasePath, "GeoLite2-City");
        this.countryReader = loadReader(countryDatabasePath, "GeoLite2-Country");
        this.asnReader = loadReader(asnDatabasePath, "GeoLite2-ASN");
    }

    private DatabaseReader loadReader(String path, String dbName) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    DatabaseReader reader = new DatabaseReader.Builder(is)
                            .withCache(new CHMCache())
                            .build();
                    log.info("Base de datos {} cargada exitosamente desde: {}", dbName, path);
                    return reader;
                }
            } else {
                log.warn("Archivo {} no encontrado en '{}'. Se omitirá en la resolución.", dbName, path);
            }
        } catch (Exception e) {
            log.error("Error al inicializar la base de datos {}: {}", dbName, e.getMessage());
        }
        return null;
    }

    @Override
    public GeoLocation resolve(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return GeoLocation.unknown("0.0.0.0");
        }

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress.trim());
        } catch (UnknownHostException e) {
            log.debug("No se pudo resolver el host para la IP {}: {}", ipAddress, e.getMessage());
            return GeoLocation.unknown(ipAddress);
        }

        // Direcciones de red local o bucle de retorno (127.0.0.1, ::1, 192.168.x.x, 10.x.x.x)
        if (inetAddress.isLoopbackAddress() || inetAddress.isSiteLocalAddress()
                || inetAddress.isLinkLocalAddress() || inetAddress.isAnyLocalAddress()) {
            return GeoLocation.local(ipAddress);
        }

        String country = "Desconocido";
        String countryCode = "N/A";
        String city = "Desconocido";
        Long asn = null;
        String asnOrganization = "Desconocido";

        // 1. Resolver Ciudad y País mediante GeoLite2-City
        if (cityReader != null) {
            try {
                Optional<CityResponse> optCity = cityReader.tryCity(inetAddress);
                if (optCity.isPresent()) {
                    CityResponse cr = optCity.get();
                    if (cr.getCountry() != null && cr.getCountry().getName() != null) {
                        country = cr.getCountry().getName();
                    }
                    if (cr.getCountry() != null && cr.getCountry().getIsoCode() != null) {
                        countryCode = cr.getCountry().getIsoCode();
                    }
                    if (cr.getCity() != null && cr.getCity().getName() != null) {
                        city = cr.getCity().getName();
                    }
                }
            } catch (Exception e) {
                log.debug("Error al consultar GeoLite2-City para {}: {}", ipAddress, e.getMessage());
            }
        }

        // 2. Fallback / Validación de País mediante GeoLite2-Country si no se resolvió país
        if ("Desconocido".equals(country) && countryReader != null) {
            try {
                Optional<CountryResponse> optCountry = countryReader.tryCountry(inetAddress);
                if (optCountry.isPresent()) {
                    CountryResponse cr = optCountry.get();
                    if (cr.getCountry() != null && cr.getCountry().getName() != null) {
                        country = cr.getCountry().getName();
                    }
                    if (cr.getCountry() != null && cr.getCountry().getIsoCode() != null) {
                        countryCode = cr.getCountry().getIsoCode();
                    }
                }
            } catch (Exception e) {
                log.debug("Error al consultar GeoLite2-Country para {}: {}", ipAddress, e.getMessage());
            }
        }

        // 3. Resolver Sistema Autónomo (ASN) y Organización / Proveedor mediante GeoLite2-ASN
        if (asnReader != null) {
            try {
                Optional<AsnResponse> optAsn = asnReader.tryAsn(inetAddress);
                if (optAsn.isPresent()) {
                    AsnResponse ar = optAsn.get();
                    if (ar.getAutonomousSystemNumber() != null) {
                        asn = ar.getAutonomousSystemNumber().longValue();
                    }
                    if (ar.getAutonomousSystemOrganization() != null) {
                        asnOrganization = ar.getAutonomousSystemOrganization();
                    }
                }
            } catch (Exception e) {
                log.debug("Error al consultar GeoLite2-ASN para {}: {}", ipAddress, e.getMessage());
            }
        }

        return new GeoLocation(ipAddress, country, countryCode, city, asn, asnOrganization);
    }

    @PreDestroy
    public void close() {
        closeReader(cityReader, "GeoLite2-City");
        closeReader(countryReader, "GeoLite2-Country");
        closeReader(asnReader, "GeoLite2-ASN");
    }

    private void closeReader(DatabaseReader reader, String name) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.debug("Error al cerrar reader {}: {}", name, e.getMessage());
            }
        }
    }
}
