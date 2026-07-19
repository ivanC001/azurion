package com.azurion.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class TenantFilterTest {

    private final TenantSchemaLookupService lookupService = mock(TenantSchemaLookupService.class);
    private final TenantFilter filter = new TenantFilter(lookupService);

    TenantFilterTest() {
        ReflectionTestUtils.setField(filter, "tenantHeader", "X-Tenant-Id");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void validatesAnActiveTenantBeforeContinuing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", " Tenant_Demo ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(lookupService.resolveSchema("tenant_demo")).thenReturn("tenant_demo");

        filter.doFilter(request, response, chain);

        verify(lookupService).resolveSchema("tenant_demo");
        verify(chain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT);
    }

    @Test
    void rejectsMalformedTenantBeforeAccessingTheRegistry() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "../../otro-schema");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("TENANT_ACCESS_DENIED");
        verify(lookupService, never()).resolveSchema("../../otro-schema");
        verify(chain, never()).doFilter(request, response);
    }
}
