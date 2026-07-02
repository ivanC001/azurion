package com.azurion.saascore.configuracion.application.usecases;

import com.azurion.multitenancy.TenantMigrationService;
import com.azurion.saascore.configuracion.application.dto.EmpresaModuloAssignmentRequest;
import com.azurion.saascore.configuracion.application.dto.EmpresaModuloResponse;
import com.azurion.saascore.configuracion.application.dto.SyncEmpresaModulosRequest;
import com.azurion.saascore.configuracion.application.mappers.EmpresaModuloMapper;
import com.azurion.saascore.configuracion.application.services.EmpresaModuloAdminAccessService;
import com.azurion.saascore.configuracion.domain.entities.EmpresaModulo;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.application.services.PlatformModuleAuditService;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsignarModulosEmpresaUseCase {

    private final EmpresaModuloRepository empresaModuloRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloRepository moduloRepository;
    private final EmpresaModuloAdminAccessService accessService;
    private final PlatformModuleAuditService auditService;
    private final TenantMigrationService tenantMigrationService;
    private final ListEmpresaModulosUseCase listEmpresaModulosUseCase;

    @Transactional
    public List<EmpresaModuloResponse> execute(Long empresaId, SyncEmpresaModulosRequest request) {
        accessService.requireWriteAccess(empresaId);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("EMPRESA_NOT_FOUND", "Empresa not found: " + empresaId));

        for (EmpresaModuloAssignmentRequest item : request.modulos()) {
            Modulo modulo = resolveModulo(item);

            EmpresaModulo empresaModulo = empresaModuloRepository.findByEmpresaIdAndModuloId(empresaId, modulo.getId())
                    .orElseGet(EmpresaModulo::new);

            empresaModulo.setEmpresa(empresa);
            empresaModulo.setModulo(modulo);
            empresaModulo.setActivo(Boolean.TRUE.equals(item.activo()));
            empresaModulo.setEstado(normalizeEstado(item.estado(), item.activo()));
            empresaModulo.setFechaInicio(item.fechaInicio() == null ? LocalDate.now() : item.fechaInicio());
            empresaModulo.setFechaFin(item.fechaFin());
            empresaModulo.setConfiguracionExtra(item.configuracionExtra());

            empresaModuloRepository.save(empresaModulo);
        }

        tenantMigrationService.migrateSchema(
                empresa.getSchemaName(),
                empresaModuloRepository.findActiveModuleCodes(empresaId, LocalDate.now()),
                false
        );

        auditService.record(
                "/companies/" + empresaId + "/modules",
                "Cambio de modulos contratados de empresa " + empresa.getTenantId()
        );

        return listEmpresaModulosUseCase.execute(empresaId);
    }

    private Modulo resolveModulo(EmpresaModuloAssignmentRequest item) {
        if (item.moduloId() != null) {
            return moduloRepository.findById(item.moduloId())
                    .orElseThrow(() -> new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + item.moduloId()));
        }
        if (item.moduloCodigo() != null && !item.moduloCodigo().isBlank()) {
            return moduloRepository.findByCodigoIgnoreCase(item.moduloCodigo())
                    .orElseThrow(() -> new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + item.moduloCodigo()));
        }
        throw new BusinessException("MODULO_REQUIRED", "Debe indicar moduloId o moduloCodigo");
    }

    private String normalizeEstado(String estado, Boolean activo) {
        if (estado != null && !estado.isBlank()) {
            return estado.trim().toUpperCase();
        }
        return Boolean.TRUE.equals(activo) ? "ACTIVO" : "INACTIVO";
    }
}
