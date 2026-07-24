package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CrmWhatsappConversationResponse(
        Long id,
        Long prospectoId,
        String nombre,
        String telefono,
        String correo,
        String direccion,
        String origen,
        String canalIngreso,
        String campania,
        String interesPrincipal,
        String estadoProspecto,
        String nivelInteres,
        String responsableId,
        String estadoConversacion,
        Integer noLeidos,
        String ultimoMensaje,
        String ultimaDireccion,
        OffsetDateTime ultimoMensajeEn,
        String notaInterna,
        List<CrmWhatsappInternalNoteResponse> notasInternas
) {
}
