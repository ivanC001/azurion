package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record ScheduleWhatsappReengagementRequest(
        @NotBlank @Size(max = 512) String nombre,
        @NotBlank @Size(max = 35) String idioma,
        @NotNull @Size(max = 30) List<@Size(max = 1024) String> parametros,
        @NotNull LocalDateTime programadoPara
) {
}
