package com.azurion.saascore.auth.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azurion.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthorizationServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crmAdminDoesNotBypassErpResourceAssignments() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(query);
        when(query.setParameter(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(query);
        when(query.getSingleResult()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "crm-admin",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CRM_ADMIN"))
        ));

        AuthorizationService service = new AuthorizationService(entityManager);

        assertThatThrownBy(() -> service.validarCaja(15L, 99L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo("CAJA_NO_ASIGNADA"));
    }
}
