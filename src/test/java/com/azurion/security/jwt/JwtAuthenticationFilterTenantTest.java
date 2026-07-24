package com.azurion.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

import com.azurion.multitenancy.TenantContext;
import com.azurion.security.session.AuthSessionService;
import com.azurion.security.session.SessionRevokedException;
import com.azurion.security.session.SessionStoreUnavailableException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTenantTest {

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final AuthSessionService authSessionService = mock(AuthSessionService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(tokenProvider, authSessionService);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsGeneralAdministratorToSwitchToAnActiveTenant() throws Exception {
        Claims claims = claims("public", 7L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_GENERAL")))
                .when(tokenProvider).authoritiesFrom(claims);

        TenantContext.setTenantId("tenant_demo");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        TenantAuthenticationDetails details = (TenantAuthenticationDetails)
                SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertThat(details.getTenantId()).isEqualTo("tenant_demo");
        assertThat(details.getSessionTenantId()).isEqualTo("public");
        assertThat(details.getUserId()).isEqualTo(7L);
    }

    @Test
    void allowsPlatformAdministratorToSwitchToAnActiveTenant() throws Exception {
        Claims claims = claims("public", 7L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")))
                .when(tokenProvider).authoritiesFrom(claims);

        TenantContext.setTenantId("tenant_demo");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        TenantAuthenticationDetails details = (TenantAuthenticationDetails)
                SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertThat(details.getTenantId()).isEqualTo("tenant_demo");
        assertThat(details.getSessionTenantId()).isEqualTo("public");
        assertThat(details.getUserId()).isEqualTo(7L);
    }

    @Test
    void rejectsTenantUserSwitchToAnotherTenant() throws Exception {
        Claims claims = claims("tenant_origin", 8L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")))
                .when(tokenProvider).authoritiesFrom(claims);

        TenantContext.setTenantId("tenant_other");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TENANT_ACCESS_DENIED");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void reportsRevokedSessionWithStableErrorCode() throws Exception {
        Claims claims = claims("tenant_origin", 8L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")))
                .when(tokenProvider).authoritiesFrom(claims);
        doThrow(new SessionRevokedException())
                .when(authSessionService).validate(
                        org.mockito.ArgumentMatchers.eq("tenant_origin"),
                        org.mockito.ArgumentMatchers.eq(8L),
                        org.mockito.ArgumentMatchers.eq("session-id"),
                        any(com.azurion.security.session.SessionClientInfo.class)
                );
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("SESSION_REVOKED");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() throws Exception {
        Claims claims = claims("tenant_origin", 8L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA")))
                .when(tokenProvider).authoritiesFrom(claims);
        doThrow(new SessionStoreUnavailableException(new RuntimeException("offline")))
                .when(authSessionService).validate(
                        org.mockito.ArgumentMatchers.eq("tenant_origin"),
                        org.mockito.ArgumentMatchers.eq(8L),
                        org.mockito.ArgumentMatchers.eq("session-id"),
                        any(com.azurion.security.session.SessionClientInfo.class)
                );
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SESSION_SERVICE_UNAVAILABLE");
        verify(chain, never()).doFilter(request, response);
    }

    private Claims claims(String tenant, Long userId) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("platform.admin");
        when(claims.getOrDefault("tenant", TenantContext.DEFAULT_TENANT)).thenReturn(tenant);
        when(claims.getOrDefault("userId", null)).thenReturn(userId);
        when(claims.getOrDefault("sid", "")).thenReturn("session-id");
        return claims;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/saas/clientes");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
