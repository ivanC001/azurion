package com.azurion.saascore.facturacion.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.application.dto.FacturadorTenantConfigurationRequest;
import com.azurion.saascore.facturacion.application.services.FacturadorTenantPayloadFactory;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManageCurrentEmpresaFacturadorUseCaseTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final SynchronizeCurrentEmpresaFacturadorUseCase synchronizeUseCase =
            mock(SynchronizeCurrentEmpresaFacturadorUseCase.class);
    private final FacturadorClient facturadorClient = mock(FacturadorClient.class);
    private final FacturadorTenantPayloadFactory payloadFactory = mock(FacturadorTenantPayloadFactory.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ManageCurrentEmpresaFacturadorUseCase useCase =
            new ManageCurrentEmpresaFacturadorUseCase(
                    empresaRepository,
                    synchronizeUseCase,
                    facturadorClient,
                    payloadFactory
            );

    private Empresa empresa;
    private List<Map<String, String>> accounts;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant_demo");
        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setTenantId("tenant_demo");
        empresa.setRuc("20601234567");
        empresa.setRazonSocial("EMPRESA DEMO SAC");
        empresa.setPaisCodigo("PE");
        empresa.setSchemaName("tenant_demo");

        accounts = List.of(Map.of(
                "banco", "BCP",
                "moneda", "PEN",
                "cuenta", "191-1234567-0-12",
                "cci", "00219100123456789012"
        ));

        when(empresaRepository.findByTenantId("tenant_demo")).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rejectsSuccessWhenFacturadorDoesNotReturnRequestedBankAccounts() throws Exception {
        FacturadorTenantConfigurationRequest request = new FacturadorTenantConfigurationRequest();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cuentas_bancarias", accounts);
        when(payloadFactory.create(request)).thenReturn(payload);
        when(facturadorClient.actualizarTenantAdministradoPorExternalId(eq("tenant_demo"), anyMap()))
                .thenReturn(objectMapper.readTree("""
                        {"configuracion":{"cuentas_bancarias":[]}}
                        """));

        assertThatThrownBy(() -> useCase.updateConfiguration(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no confirmo el guardado");
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    void acceptsSuccessOnlyWhenFacturadorReturnsThePersistedBankAccounts() throws Exception {
        FacturadorTenantConfigurationRequest request = new FacturadorTenantConfigurationRequest();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cuentas_bancarias", accounts);
        when(payloadFactory.create(request)).thenReturn(payload);
        JsonNode tenant = objectMapper.readTree("""
                {
                  "document_mode":"electronic",
                  "fiscal_status":"active",
                  "sunat_mode":"beta",
                  "configuracion":{"cuentas_bancarias":[{
                    "banco":"BCP",
                    "moneda":"PEN",
                    "cuenta":"191-1234567-0-12",
                    "cci":"00219100123456789012"
                  }]}
                }
                """);
        when(facturadorClient.actualizarTenantAdministradoPorExternalId(eq("tenant_demo"), anyMap()))
                .thenReturn(tenant);

        var result = useCase.updateConfiguration(request);

        assertThat(result.tenant()).isSameAs(tenant);
        assertThat(result.tenant().path("configuracion").path("cuentas_bancarias")).hasSize(1);
        verify(empresaRepository).save(empresa);
    }
}
