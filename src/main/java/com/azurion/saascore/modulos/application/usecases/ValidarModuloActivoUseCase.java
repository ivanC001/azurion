package com.azurion.saascore.modulos.application.usecases;

import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidarModuloActivoUseCase {

    private final ModuleAccessService moduleAccessService;

    public boolean execute(Long empresaId, String moduloCodigo) {
        return moduleAccessService.hasModule(empresaId, moduloCodigo);
    }

    public void require(Long empresaId, String moduloCodigo) {
        moduleAccessService.requireModule(empresaId, moduloCodigo);
    }
}
