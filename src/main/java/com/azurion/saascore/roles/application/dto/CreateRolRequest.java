package com.azurion.saascore.roles.application.dto;

import com.azurion.saascore.roles.domain.entities.RoleScope;
import jakarta.validation.constraints.NotBlank;

public record CreateRolRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String descripcion,
        RoleScope ambito
) {
}
