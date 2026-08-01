package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.mappers.EmpresaMapper;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.application.dto.CurrentFacturadorConfigurationResponse;
import com.azurion.saascore.facturacion.application.dto.FacturadorTenantConfigurationRequest;
import com.azurion.saascore.facturacion.application.services.FacturadorTenantPayloadFactory;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManageCurrentEmpresaFacturadorUseCase {

    private final EmpresaRepository empresaRepository;
    private final SynchronizeCurrentEmpresaFacturadorUseCase synchronizeUseCase;
    private final FacturadorClient facturadorClient;
    private final FacturadorTenantPayloadFactory payloadFactory;

    public JsonNode getConfiguration() {
        Empresa empresa = currentEmpresa();
        return facturadorClient.obtenerTenantAdministradoPorExternalId(empresa.getTenantId());
    }

    public CurrentFacturadorConfigurationResponse updateConfiguration(
            FacturadorTenantConfigurationRequest request
    ) {
        Empresa empresa = currentEmpresa();

        // Un unico aprovisionamiento idempotente garantiza que el tenant exista antes de actualizarlo.
        synchronizeUseCase.execute();

        Map<String, Object> payload = payloadFactory.create(request);
        payload.put("ruc", empresa.getRuc());
        payload.put("business_name", empresa.getRazonSocial());
        payload.put("external_tenant_id", empresa.getTenantId());
        payload.put("country_code", empresa.getPaisCodigo());
        payload.put("tax_id", empresa.getRuc());

        JsonNode tenant = facturadorClient.actualizarTenantAdministradoPorExternalId(
                empresa.getTenantId(),
                payload
        );

        Empresa refreshed = currentEmpresa();
        applyFacturadorCapabilities(refreshed, tenant);
        EmpresaResponse empresaResponse = EmpresaMapper.toResponse(empresaRepository.save(refreshed));
        return new CurrentFacturadorConfigurationResponse(tenant, empresaResponse);
    }

    private Empresa currentEmpresa() {
        return empresaRepository.findByTenantId(TenantContext.getTenantId())
                .orElseThrow(() -> new BusinessException(
                        "EMPRESA_NO_ENCONTRADA",
                        "Empresa no encontrada para el tenant actual"
                ));
    }

    private void applyFacturadorCapabilities(Empresa empresa, JsonNode tenant) {
        empresa.setFacturadorStatus(Empresa.FACTURADOR_STATUS_PROVISIONADO);
        empresa.setFacturadorDocumentMode(upperOrDefault(
                text(tenant, "document_mode"),
                Empresa.FACTURADOR_DOCUMENT_MODE_TICKET_ONLY
        ));
        empresa.setFacturadorFiscalStatus(upperOrDefault(
                text(tenant, "fiscal_status"),
                Empresa.FACTURADOR_FISCAL_STATUS_NOT_CONFIGURED
        ));
        empresa.setFacturadorSunatMode(upperOrDefault(
                text(tenant, "sunat_mode"),
                Empresa.FACTURADOR_SUNAT_MODE_DISABLED
        ));
        empresa.setFacturadorLastError(null);
        empresa.setFacturadorProvisionedAt(OffsetDateTime.now());
        empresa.setFacturadorNextAttemptAt(null);
        empresa.setFacturadorLeaseOwner(null);
        empresa.setFacturadorLeaseUntil(null);
    }

    private String text(JsonNode source, String field) {
        if (source == null || source.path(field).isNull() || source.path(field).isMissingNode()) {
            return null;
        }
        String value = source.path(field).asText("").trim();
        return value.isBlank() ? null : value;
    }

    private String upperOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }
}
