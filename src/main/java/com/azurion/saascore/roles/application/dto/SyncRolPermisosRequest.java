package com.azurion.saascore.roles.application.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SyncRolPermisosRequest(
        @NotEmpty List<Long> permisoIds
) {
}
