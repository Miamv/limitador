package ar.edu.um.ingenieria.limitador.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver();
    }

    @Test
    void extractClientIp_whenRequestIsNull_returnsDefaultIp() {
        assertThat(resolver.extractClientIp(null)).isEqualTo("0.0.0.0");
    }

    @Test
    void extractClientIp_whenXForwardedForPresent_returnsFirstIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        request.setRemoteAddr("10.0.0.1");

        assertThat(resolver.extractClientIp(request)).isEqualTo("203.0.113.195");
    }

    @Test
    void extractClientIp_whenXRealIpPresent_returnsIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.22");
        request.setRemoteAddr("10.0.0.1");

        assertThat(resolver.extractClientIp(request)).isEqualTo("198.51.100.22");
    }

    @Test
    void extractClientIp_whenNoHeaders_fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("181.164.20.5");

        assertThat(resolver.extractClientIp(request)).isEqualTo("181.164.20.5");
    }

    @Test
    void extractClientIp_whenIpv6Loopback_convertsToIpv4Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        assertThat(resolver.extractClientIp(request)).isEqualTo("127.0.0.1");

        request.setRemoteAddr("::1");
        assertThat(resolver.extractClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void extractClientIp_whenHeaderIsUnknown_skipsAndUsesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.setRemoteAddr("181.164.20.5");

        assertThat(resolver.extractClientIp(request)).isEqualTo("181.164.20.5");
    }
}
