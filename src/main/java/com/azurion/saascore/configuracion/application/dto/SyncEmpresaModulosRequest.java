package com.azurion.saascore.configuracion.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SyncEmpresaModulosRequest(
        @Valid @NotEmpty List<EmpresaModuloAssignmentRequest> modulos
) {
}
