package ar.edu.um.ingenieria.limitador.util;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            "X-Real-IP"
    };

    public String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList)) {
                String candidate = ipList.split(",")[0].trim();
                if (IpCidrValidator.isValidIp(candidate)) {
                    return normalizeLoopback(candidate);
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isBlank() && IpCidrValidator.isValidIp(remoteAddr)) {
            return normalizeLoopback(remoteAddr);
        }

        return "0.0.0.0";
    }

    private String normalizeLoopback(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
