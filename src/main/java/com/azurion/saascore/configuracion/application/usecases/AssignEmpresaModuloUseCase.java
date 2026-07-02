package com.azurion.saascore.configuracion.application.usecases;

import com.azurion.saascore.configuracion.application.dto.AssignEmpresaModuloRequest;
import com.azurion.saascore.configuracion.application.dto.EmpresaModuloResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignEmpresaModuloUseCase {

    private final EmpresaModuloRepository empresaModuloRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloRepository moduloRepository;
    private final EmpresaModuloAdminAccessService accessService;
    private final PlatformModuleAuditService auditService;

    @Transactional
    public EmpresaModuloResponse execute(Long empresaId, AssignEmpresaModuloRequest request) {
        accessService.requireWriteAccess(empresaId);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("EMPRESA_NOT_FOUND", "Empresa not found: " + empresaId));

        Modulo modulo = moduloRepository.findById(request.moduloId())
                .orElseThrow(() -> new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + request.moduloId()));

        EmpresaModulo empresaModulo = empresaModuloRepository.findByEmpresaIdAndModuloId(empresaId, request.moduloId())
                .orElseGet(EmpresaModulo::new);

        empresaModulo.setEmpresa(empresa);
        empresaModulo.setModulo(modulo);
        empresaModulo.setActivo(request.activo());
        empresaModulo.setEstado(request.activo() ? "ACTIVO" : "INACTIVO");
        empresaModulo.setFechaInicio(request.fechaInicio() == null ? LocalDate.now() : request.fechaInicio());
        empresaModulo.setFechaFin(request.fechaFin());
        empresaModulo.setConfiguracionExtra(request.configuracionExtra());

        EmpresaModulo saved = empresaModuloRepository.save(empresaModulo);
        auditService.record(
                "/companies/" + empresaId + "/modules/" + modulo.getCodigo(),
                "Cambio de modulo contratado " + modulo.getCodigo() + " para empresa " + empresa.getTenantId()
        );
        return EmpresaModuloMapper.toResponse(saved);
    }
}
