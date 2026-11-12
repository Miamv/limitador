package ar.edu.um.ingenieria.limitador.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IpCidrValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "192.168.1.1",
            "10.0.0.1",
            "0.0.0.0",
            "255.255.255.255",
            "127.0.0.1",
            "181.164.20.5"
    })
    @DisplayName("Valida IPv4 individuales válidas")
    void isValidIp_validIpv4_returnsTrue(String ip) {
        assertThat(IpCidrValidator.isValidIp(ip)).isTrue();
        assertThat(IpCidrValidator.isValidIpOrCidr(ip)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "::1",
            "2001:db8::1",
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "fe80::1ff:fe23:4567:890a"
    })
    @DisplayName("Valida IPv6 individuales válidas")
    void isValidIp_validIpv6_returnsTrue(String ip) {
        assertThat(IpCidrValidator.isValidIp(ip)).isTrue();
        assertThat(IpCidrValidator.isValidIpOrCidr(ip)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "256.0.0.1",
            "192.168.1",
            "192.168.1.1.1",
            "abc.def.ghi.jkl",
            "-1.0.0.1",
            "192.168.01.1",
            "2001:xyz::1",
            "not-an-ip",
            "<script>",
            "localhost"
    })
    @DisplayName("Rechaza IPs inválidas")
    void isValidIp_invalidIp_returnsFalse(String ip) {
        assertThat(IpCidrValidator.isValidIp(ip)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "192.168.1.0/24",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.1.1/32",
            "0.0.0.0/0"
    })
    @DisplayName("Valida rangos CIDR IPv4 correctos")
    void isValidIpOrCidr_validIpv4Cidr_returnsTrue(String cidr) {
        assertThat(IpCidrValidator.isValidIpOrCidr(cidr)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2001:db8::/32",
            "2001:db8::/64",
            "::1/128",
            "fe80::/10"
    })
    @DisplayName("Valida rangos CIDR IPv6 correctos")
    void isValidIpOrCidr_validIpv6Cidr_returnsTrue(String cidr) {
        assertThat(IpCidrValidator.isValidIpOrCidr(cidr)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "192.168.1.0/33",
            "192.168.1.0/-1",
            "192.168.1.0/abc",
            "192.168.1.0/",
            "/24",
            "2001:db8::/129",
            "not-an-ip/24"
    })
    @DisplayName("Rechaza rangos CIDR con máscaras o IPs inválidas")
    void isValidIpOrCidr_invalidCidr_returnsFalse(String cidr) {
        assertThat(IpCidrValidator.isValidIpOrCidr(cidr)).isFalse();
    }
}
