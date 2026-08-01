package com.azurion.saascore.facturacion.application.services;

import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FacturadorTenantProvisioningService {

    private static final String ERP_MODULE = "ERP";

    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository empresaModuloRepository;

    public void synchronizeForModules(Empresa empresa, Collection<String> activeModuleCodes) {
        boolean erpActive = activeModuleCodes != null && activeModuleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .anyMatch(ERP_MODULE::equals);

        if (!erpActive) {
            scheduleSuspension(empresa);
            return;
        }

        if (Empresa.FACTURADOR_STATUS_PROVISIONADO.equals(empresa.getFacturadorStatus())
                || Empresa.FACTURADOR_STATUS_PROVISIONANDO.equals(empresa.getFacturadorStatus())) {
            return;
        }

        enqueue(empresa);
    }

    public void enqueueProfileSynchronization(Empresa empresa) {
        if (empresa.getId() == null
                || !empresaModuloRepository.existsActiveModule(empresa.getId(), ERP_MODULE, LocalDate.now())) {
            return;
        }
        enqueue(empresa);
    }

    private void enqueue(Empresa empresa) {
        empresa.setFacturadorStatus(Empresa.FACTURADOR_STATUS_PENDIENTE);
        empresa.setFacturadorLastError(null);
        empresa.setFacturadorAttempts(0);
        empresa.setFacturadorNextAttemptAt(OffsetDateTime.now());
        empresa.setFacturadorLeaseOwner(null);
        empresa.setFacturadorLeaseUntil(null);
        empresaRepository.save(empresa);
    }

    private void scheduleSuspension(Empresa empresa) {
        if (Empresa.FACTURADOR_STATUS_NO_REQUERIDO.equals(empresa.getFacturadorStatus())
                || Empresa.FACTURADOR_STATUS_SUSPENDIDO.equals(empresa.getFacturadorStatus())) {
            return;
        }
        enqueue(empresa);
    }
}
