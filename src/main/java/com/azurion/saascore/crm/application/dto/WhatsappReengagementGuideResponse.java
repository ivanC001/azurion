package com.azurion.saascore.crm.application.dto;

import java.util.List;

/**
 * Guia de puesta en marcha del reenganche de WhatsApp.
 *
 * <p>Mira el catalogo real del WABA del tenant y responde que falta para poder
 * programar envios, en vez de dejar al usuario adivinando por que su plantilla no
 * aparece en la lista.
 *
 * @param listoParaProgramar   hay al menos una plantilla aprobada y enviable
 * @param resumen              una linea con el estado actual
 * @param pasos                que hacer, en orden
 * @param advertencias         problemas detectados en el catalogo del tenant
 * @param plantillaSugerida    modelo de plantilla listo para copiar y pegar en Meta
 * @param plantillasUtilizables las que ya se pueden usar hoy para reenganchar
 */
public record WhatsappReengagementGuideResponse(
        boolean listoParaProgramar,
        String resumen,
        List<String> pasos,
        List<String> advertencias,
        PlantillaSugerida plantillaSugerida,
        List<CrmWhatsappTemplateResponse> plantillasUtilizables
) {

    /**
     * @param motivoCategoria por que conviene esa categoria y no la otra
     * @param variables       que valor llena cada variable, en orden
     */
    public record PlantillaSugerida(
            String categoria,
            String motivoCategoria,
            String cuerpo,
            List<String> botones,
            List<String> variables
    ) {
    }
}
