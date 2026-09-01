package com.azurion.saascore.crm.application.dto;

import java.util.List;

public record CrmWhatsappTemplateResponse(
        String nombre,
        String idioma,
        String categoria,
        String cuerpo,
        int cantidadParametros,
        String id,
        String estado,
        boolean disponible,
        String motivoNoDisponible,
        List<Componente> componentes
) {
    public record Componente(String tipo, String texto, List<String> parametros) {}
}
