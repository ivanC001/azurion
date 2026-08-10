package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CrmPhoneNormalizationServiceTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void normalizaTelefonoLocalYConPrefijoAUnaMismaIdentidad() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        Empresa empresa = new Empresa();
        empresa.setPaisCodigo("PE");
        TenantContext.setTenantId("empresa_demo");
        when(empresaRepository.findByTenantId("empresa_demo")).thenReturn(Optional.of(empresa));
        CrmPhoneNormalizationService service = new CrmPhoneNormalizationService(empresaRepository);

        var local = service.normalize("999 999 999");
        var international = service.normalize("+51 999 999 999");

        assertEquals("51999999999", local.identity());
        assertEquals(local.identity(), international.identity());
        assertTrue(international.lookupCandidates().contains("999999999"));
    }

    @Test
    void usaElPaisDelProspectoAntesQueElPaisDelTenant() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        Empresa empresa = new Empresa();
        empresa.setPaisCodigo("PE");
        TenantContext.setTenantId("empresa_demo");
        when(empresaRepository.findByTenantId("empresa_demo")).thenReturn(Optional.of(empresa));
        CrmPhoneNormalizationService service = new CrmPhoneNormalizationService(empresaRepository);

        var mexicanPhone = service.normalize("55 1234 5678", "MX");

        assertEquals("525512345678", mexicanPhone.identity());
    }
}
