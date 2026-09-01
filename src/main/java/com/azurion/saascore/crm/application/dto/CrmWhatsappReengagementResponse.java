package com.azurion.saascore.crm.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CrmWhatsappReengagementResponse(
        Long id,
        Long prospectoId,
        String plantillaNombre,
        String plantillaIdioma,
        List<String> parametros,
        LocalDateTime programadoPara,
        String estado,
        Integer intentos,
        String resultado,
        String ultimoError,
        LocalDateTime procesadoEn,
        String creadoPor
) {
}
