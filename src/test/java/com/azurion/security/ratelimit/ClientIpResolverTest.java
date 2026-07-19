package com.azurion.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void resolvesTheRealClientBehindATrustedNginxProxy() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,10.0.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.24, 10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.24");
    }

    @Test
    void ignoresSpoofedForwardedHeadersFromUntrustedClients() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void usesXRealIpOnlyForATrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "192.0.2.18");

        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.18");
    }
}
