package com.azurion.saascore.usuarios.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SyncUsuarioRolesRequest(
        @NotNull List<String> rolCodigos
) {
}
