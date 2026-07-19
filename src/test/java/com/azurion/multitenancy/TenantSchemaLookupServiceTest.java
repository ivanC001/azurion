package com.azurion.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.azurion.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class TenantSchemaLookupServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TenantSchemaLookupService service = new TenantSchemaLookupService(jdbcTemplate);

    @Test
    void resolvesPublicWithoutQueryingTheRegistry() {
        assertThat(service.resolveSchema("public")).isEqualTo("public");
        assertThat(service.resolveSchema(" ")).isEqualTo("public");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsClosedForAnUnknownOrInactiveTenant() {
        when(jdbcTemplate.query(
                anyString(),
                any(ResultSetExtractor.class),
                eq("missing-tenant")
        )).thenReturn(null);

        assertThatThrownBy(() -> service.resolveSchema("missing-tenant"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("TENANT_NO_ENCONTRADO");
    }
}
