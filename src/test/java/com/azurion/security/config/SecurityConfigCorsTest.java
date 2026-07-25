package com.azurion.security.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.azurion.multitenancy.TenantFilter;
import com.azurion.security.jwt.JwtAuthenticationFilter;
import com.azurion.security.ratelimit.RateLimitFilter;
import com.azurion.shared.audit.AuditTrailFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigCorsTest {

    @Test
    void publicFormsAllowAnyOriginWithoutCredentials() {
        CorsConfigurationSource source = configurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/v1/public/forms/lnd_test/submissions"
        );
        request.setServletPath("/v1/public/forms/lnd_test/submissions");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertEquals(List.of("*"), cors.getAllowedOrigins());
        assertEquals(List.of("POST", "OPTIONS"), cors.getAllowedMethods());
        assertFalse(Boolean.TRUE.equals(cors.getAllowCredentials()));
    }

    @Test
    void administrativeApiKeepsRestrictedCredentialedCors() {
        CorsConfigurationSource source = configurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/v1/saas/crm/configuracion/landings"
        );
        request.setServletPath("/v1/saas/crm/configuracion/landings");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertEquals(List.of("https://panel.azurion.test"), cors.getAllowedOriginPatterns());
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
    }

    private CorsConfigurationSource configurationSource() {
        SecurityConfig configuration = new SecurityConfig(
                mock(TenantFilter.class),
                mock(JwtAuthenticationFilter.class),
                mock(RateLimitFilter.class),
                mock(AuditTrailFilter.class)
        );
        ReflectionTestUtils.setField(
                configuration,
                "corsAllowedOriginsProperty",
                "https://panel.azurion.test"
        );
        return configuration.corsConfigurationSource();
    }
}
