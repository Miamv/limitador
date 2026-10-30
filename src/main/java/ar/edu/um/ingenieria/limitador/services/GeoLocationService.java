package ar.edu.um.ingenieria.limitador.services;

import ar.edu.um.ingenieria.limitador.dto.GeoLocation;

public interface GeoLocationService {

    GeoLocation resolve(String ipAddress);
}
