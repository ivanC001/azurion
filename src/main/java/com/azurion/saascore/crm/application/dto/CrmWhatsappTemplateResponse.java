package com.azurion.saascore.crm.application.dto;

public record CrmWhatsappTemplateResponse(
        String nombre,
        String idioma,
        String categoria,
        String cuerpo,
        int cantidadParametros
) {
}
