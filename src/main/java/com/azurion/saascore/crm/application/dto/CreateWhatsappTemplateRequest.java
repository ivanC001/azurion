package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Borrador de plantilla que el usuario compone en el CRM.
 *
 * <p>Las validaciones de forma viven en el dominio ({@code WhatsappTemplateDraft}),
 * no aca: son las mismas reglas que el compositor aplica al enviar, y tenerlas en un
 * solo lugar evita aprobar en Meta una plantilla que despues no se pueda usar.
 *
 * @param ejemploEncabezado un ejemplo por variable del encabezado, en orden
 * @param ejemploCuerpo     un ejemplo por variable del cuerpo, en orden
 */
public record CreateWhatsappTemplateRequest(
        @NotBlank @Size(max = 512) String nombre,
        @NotBlank @Size(max = 35) String idioma,
        @NotBlank @Size(max = 30) String categoria,
        @Size(max = 60) String encabezado,
        @Size(max = 30) List<@Size(max = 1024) String> ejemploEncabezado,
        @NotBlank @Size(max = 1024) String cuerpo,
        @Size(max = 30) List<@Size(max = 1024) String> ejemploCuerpo,
        @Size(max = 60) String pie,
        @Size(max = 10) List<Boton> botones
) {
    public record Boton(
            @NotBlank @Size(max = 30) String tipo,
            @NotBlank @Size(max = 25) String texto,
            @Size(max = 2000) String url,
            @Size(max = 20) String telefono
    ) {
    }
}
