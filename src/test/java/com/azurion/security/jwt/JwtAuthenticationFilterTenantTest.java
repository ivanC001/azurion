package com.azurion.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRolRepository;
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
    private final UsuarioTenantRolRepository assignmentRepository = mock(UsuarioTenantRolRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, assignmentRepository);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsPlatformTenantSwitchWithoutPersistentAssignment() throws Exception {
        Claims claims = claims("public", 7L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_GENERAL")))
                .when(tokenProvider).authoritiesFrom(claims);
        when(assignmentRepository.existsByUsuarioGlobalIdAndTenantIdIgnoreCaseAndActivoTrue(7L, "tenant_demo"))
                .thenReturn(false);

        TenantContext.setTenantId("tenant_demo");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TENANT_ACCESS_DENIED");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void storesAuthorizedTenantInAuthenticationDetails() throws Exception {
        Claims claims = claims("public", 7L);
        when(tokenProvider.parseClaims("token")).thenReturn(claims);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN_GENERAL")))
                .when(tokenProvider).authoritiesFrom(claims);
        when(assignmentRepository.existsByUsuarioGlobalIdAndTenantIdIgnoreCaseAndActivoTrue(7L, "tenant_demo"))
                .thenReturn(true);

        TenantContext.setTenantId("tenant_demo");
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        TenantAuthenticationDetails details = (TenantAuthenticationDetails)
                SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertThat(details.getTenantId()).isEqualTo("tenant_demo");
        assertThat(details.getUserId()).isEqualTo(7L);
    }

    private Claims claims(String tenant, Long userId) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("platform.admin");
        when(claims.getOrDefault("tenant", TenantContext.DEFAULT_TENANT)).thenReturn(tenant);
        when(claims.getOrDefault("userId", null)).thenReturn(userId);
        return claims;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/saas/clientes");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
