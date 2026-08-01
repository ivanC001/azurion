package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.mappers.EmpresaMapper;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SynchronizeCurrentEmpresaFacturadorUseCase {

    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final FacturadorClient facturadorClient;

    public EmpresaResponse execute() {
        String tenantId = TenantContext.getTenantId();
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "EMPRESA_NO_ENCONTRADA",
                        "Empresa no encontrada para el tenant actual"
                ));
        if (!empresaModuloRepository.existsActiveModule(empresa.getId(), "ERP", LocalDate.now())) {
            throw new BusinessException(
                    "ERP_MODULE_REQUIRED",
                    "El modulo ERP debe estar activo para usar el facturador"
            );
        }

        FacturadorClient.FacturadorTenantProvisioningResult result = facturadorClient.provisionarTenant(
                empresa.getTenantId(),
                empresa.getRazonSocial(),
                empresa.getPaisCodigo(),
                empresa.getRuc(),
                empresa.isActivo()
        );

        empresa.setFacturadorStatus(Empresa.FACTURADOR_STATUS_PROVISIONADO);
        empresa.setFacturadorDocumentMode(upperOrDefault(
                result.documentMode(),
                Empresa.FACTURADOR_DOCUMENT_MODE_TICKET_ONLY
        ));
        empresa.setFacturadorFiscalStatus(upperOrDefault(
                result.fiscalStatus(),
                Empresa.FACTURADOR_FISCAL_STATUS_NOT_CONFIGURED
        ));
        empresa.setFacturadorSunatMode(upperOrDefault(
                result.sunatMode(),
                Empresa.FACTURADOR_SUNAT_MODE_DISABLED
        ));
        empresa.setFacturadorLastError(null);
        empresa.setFacturadorProvisionedAt(OffsetDateTime.now());
        empresa.setFacturadorNextAttemptAt(null);
        empresa.setFacturadorLeaseOwner(null);
        empresa.setFacturadorLeaseUntil(null);
        return EmpresaMapper.toResponse(empresaRepository.save(empresa));
    }

    private String upperOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }
}
