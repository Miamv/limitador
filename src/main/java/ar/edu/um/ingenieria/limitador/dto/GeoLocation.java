package ar.edu.um.ingenieria.limitador.dto;

public record GeoLocation(
        String ip,
        String country,
        String countryCode,
        String city,
        Long asn,
        String asnOrganization
) {

    public static GeoLocation unknown(String ip) {
        return new GeoLocation(ip, "Desconocido", "N/A", "Desconocido", null, "Desconocido");
    }

    public static GeoLocation local(String ip) {
        return new GeoLocation(ip, "Local / Red Privada", "LOC", "Localhost", null, "Red Local");
    }
}
