package com.azurion.saascore.facturacion.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacturadorTenantProvisioningServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private EmpresaModuloRepository empresaModuloRepository;

    private FacturadorTenantProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new FacturadorTenantProvisioningService(empresaRepository, empresaModuloRepository);
    }

    @Test
    void activatingErpQueuesAutomaticProvisioning() {
        Empresa empresa = empresa(Empresa.FACTURADOR_STATUS_NO_REQUERIDO);

        service.synchronizeForModules(empresa, List.of("ERP", "VENTAS"));

        assertEquals(Empresa.FACTURADOR_STATUS_PENDIENTE, empresa.getFacturadorStatus());
        verify(empresaRepository).save(empresa);
    }

    @Test
    void removingErpQueuesRemoteSuspensionForProvisionedTenant() {
        Empresa empresa = empresa(Empresa.FACTURADOR_STATUS_PROVISIONADO);

        service.synchronizeForModules(empresa, List.of("CRM"));

        assertEquals(Empresa.FACTURADOR_STATUS_PENDIENTE, empresa.getFacturadorStatus());
        verify(empresaRepository).save(empresa);
    }

    @Test
    void tenantThatNeverRequiredFacturadorDoesNotCreateRemoteTenantOnErpAbsence() {
        Empresa empresa = empresa(Empresa.FACTURADOR_STATUS_NO_REQUERIDO);

        service.synchronizeForModules(empresa, List.of("CRM"));

        assertEquals(Empresa.FACTURADOR_STATUS_NO_REQUERIDO, empresa.getFacturadorStatus());
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    void profileChangeQueuesSynchronizationOnlyWhenErpIsActive() {
        Empresa empresa = empresa(Empresa.FACTURADOR_STATUS_PROVISIONADO);
        when(empresaModuloRepository.existsActiveModule(anyLong(), anyString(), any(LocalDate.class)))
                .thenReturn(true);

        service.enqueueProfileSynchronization(empresa);

        assertEquals(Empresa.FACTURADOR_STATUS_PENDIENTE, empresa.getFacturadorStatus());
        verify(empresaRepository).save(empresa);
    }

    private Empresa empresa(String status) {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setFacturadorStatus(status);
        return empresa;
    }
}
