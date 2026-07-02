package com.azurion.saascore.modulos.application.usecases;

import com.azurion.saascore.modulos.application.dto.CreateModuloRequest;
import com.azurion.saascore.modulos.application.dto.ModuloResponse;
import com.azurion.saascore.modulos.application.mappers.ModuloMapper;
import com.azurion.saascore.modulos.application.services.PlatformModuleAuditService;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateModuloUseCase {

    private final ModuloRepository moduloRepository;
    private final PlatformModuleAuditService auditService;

    @Transactional
    public ModuloResponse execute(CreateModuloRequest request) {
        moduloRepository.findByCodigoIgnoreCase(request.codigo()).ifPresent(existing -> {
            throw new BusinessException("MODULO_CODE_EXISTS", "A modulo with same codigo already exists");
        });

        Modulo modulo = new Modulo();
        modulo.setCodigo(request.codigo().trim().toUpperCase());
        modulo.setNombre(request.nombre());
        modulo.setDescripcion(request.descripcion());
        modulo.setEstado("ACTIVO");

        Modulo saved = moduloRepository.save(modulo);
        auditService.record("/modules", "Creacion de modulo " + saved.getCodigo());
        return ModuloMapper.toResponse(saved);
    }
}
