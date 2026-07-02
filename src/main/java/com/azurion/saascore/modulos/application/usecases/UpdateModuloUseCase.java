package com.azurion.saascore.modulos.application.usecases;

import com.azurion.saascore.modulos.application.dto.ModuloResponse;
import com.azurion.saascore.modulos.application.dto.UpdateModuloRequest;
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
public class UpdateModuloUseCase {

    private final ModuloRepository moduloRepository;
    private final PlatformModuleAuditService auditService;

    @Transactional
    public ModuloResponse execute(Long id, UpdateModuloRequest request) {
        Modulo modulo = moduloRepository.findById(id)
                .orElseThrow(() -> new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + id));

        modulo.setNombre(request.nombre());
        modulo.setDescripcion(request.descripcion());
        modulo.setEstado(request.estado().trim().toUpperCase());

        Modulo saved = moduloRepository.save(modulo);
        auditService.record("/modules/" + id, "Actualizacion de modulo " + saved.getCodigo() + " a estado " + saved.getEstado());
        return ModuloMapper.toResponse(saved);
    }
}
