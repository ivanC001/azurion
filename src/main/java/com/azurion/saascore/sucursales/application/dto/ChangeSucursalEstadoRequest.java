package com.azurion.saascore.sucursales.application.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeSucursalEstadoRequest(@NotNull Boolean activo) {
}
