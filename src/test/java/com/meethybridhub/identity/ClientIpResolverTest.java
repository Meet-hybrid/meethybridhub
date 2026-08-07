package com.meethybridhub.identity;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ClientIpResolver}: the X-Forwarded-For header is
 * ignored unless explicitly trusted, and the last hop is used when it is.
 */
class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void ignoresForwardedHeaderByDefault() {
        ReflectionTestUtils.setField(resolver, "trustForwardedHeader", false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 198.51.100.4");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.7");
    }

    @Test
    void usesLastHopWhenForwardedHeaderTrusted() {
        ReflectionTestUtils.setField(resolver, "trustForwardedHeader", true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 198.51.100.4");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.4");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardedHeaderBlank() {
        ReflectionTestUtils.setField(resolver, "trustForwardedHeader", true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");
        request.addHeader("X-Forwarded-For", "   ");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.7");
    }
}
