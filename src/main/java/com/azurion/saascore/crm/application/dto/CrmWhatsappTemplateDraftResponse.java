package com.azurion.saascore.crm.application.dto;

/**
 * Lo que respondio Meta al recibir el borrador.
 *
 * @param estado  normalmente PENDING: la plantilla no se puede usar hasta que Meta la aprueba
 * @param mensaje texto listo para mostrarle al usuario
 */
public record CrmWhatsappTemplateDraftResponse(
        String id,
        String nombre,
        String idioma,
        String categoria,
        String estado,
        String mensaje
) {
}
