package com.azurion.saascore.modulos.application.usecases;

import com.azurion.saascore.modulos.application.dto.ActiveModulesResponse;
import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObtenerModulosActivosEmpresaUseCase {

    private final ModuleAccessService moduleAccessService;

    public ActiveModulesResponse execute(Long empresaId) {
        List<String> modules = moduleAccessService.getActiveModules(empresaId);
        return new ActiveModulesResponse(empresaId, null, modules);
    }

    public ActiveModulesResponse executeCurrentTenant() {
        Long empresaId = moduleAccessService.getCurrentTenantEmpresaId();
        return new ActiveModulesResponse(
                empresaId,
                moduleAccessService.getCurrentTenantId(),
                moduleAccessService.getActiveModules(empresaId)
        );
    }
}
