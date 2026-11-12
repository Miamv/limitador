package ar.edu.um.ingenieria.limitador.util;

import java.net.InetAddress;

/**
 * Validador estricto de direcciones IPv4, IPv6 y notación de subredes CIDR.
 * No realiza consultas DNS para evitar demoras y ataques de suplantación.
 */
public final class IpCidrValidator {

    private IpCidrValidator() {
    }

    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String trimmed = ip.trim();
        return isValidIpv4(trimmed) || isValidIpv6(trimmed);
    }

    public static boolean isValidIpOrCidr(String ipOrCidr) {
        if (ipOrCidr == null || ipOrCidr.isBlank()) {
            return false;
        }
        String trimmed = ipOrCidr.trim();
        int slashIndex = trimmed.indexOf('/');
        if (slashIndex == -1) {
            return isValidIp(trimmed);
        }

        String ipPart = trimmed.substring(0, slashIndex).trim();
        String maskPart = trimmed.substring(slashIndex + 1).trim();

        if (maskPart.isEmpty() || !maskPart.chars().allMatch(Character::isDigit)) {
            return false;
        }

        int mask;
        try {
            mask = Integer.parseInt(maskPart);
        } catch (NumberFormatException e) {
            return false;
        }

        if (isValidIpv4(ipPart)) {
            return mask >= 0 && mask <= 32;
        } else if (isValidIpv6(ipPart)) {
            return mask >= 0 && mask <= 128;
        }

        return false;
    }

    public static boolean isValidIpv4(String ip) {
        if (ip == null) {
            return false;
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            if (part.length() > 1 && part.startsWith("0")) {
                return false; // Evita ambigüedad con octal
            }
            for (char c : part.toCharArray()) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            }
            int val = Integer.parseInt(part);
            if (val < 0 || val > 255) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidIpv6(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String cleaned = ip.contains("%") ? ip.substring(0, ip.indexOf('%')) : ip;
        // Comprobar caracteres válidos en IPv6 para garantizar que InetAddress no haga lookup DNS
        for (char c : cleaned.toCharArray()) {
            if (c != ':' && c != '.' && Character.digit(c, 16) == -1) {
                return false;
            }
        }
        try {
            byte[] bytes = InetAddress.getByName(cleaned).getAddress();
            return bytes.length == 16;
        } catch (Exception e) {
            return false;
        }
    }
}
