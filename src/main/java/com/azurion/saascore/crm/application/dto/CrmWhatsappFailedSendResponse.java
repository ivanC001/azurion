package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

/**
 * Un envio que Meta no pudo entregar, listo para mostrar.
 *
 * @param causa    explicacion en castellano del codigo de Meta, si lo conocemos
 * @param solucion siguiente paso concreto, si lo hay
 * @param detalle  texto original de Meta, para los casos que no tenemos traducidos
 */
public record CrmWhatsappFailedSendResponse(
        Long mensajeId,
        Long prospectoId,
        String prospectoNombre,
        String tipoMensaje,
        String plantillaNombre,
        String codigo,
        String causa,
        String solucion,
        String detalle,
        OffsetDateTime ocurrioEn
) {
}
