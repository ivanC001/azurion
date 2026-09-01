package com.azurion.saascore.crm.infrastructure.http;

import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Traduce los rechazos de WhatsApp Cloud API que el operador puede corregir.
 *
 * <p>El texto que devuelve Meta no se expone nunca: {@code CRM_WHATSAPP_META_ERROR}
 * esta en la lista interna de {@code ErrorExposurePolicy} justamente para que un
 * mensaje de Graph no llegue al navegador. Esa proteccion se mantiene para todo lo
 * desconocido; aqui solo se reescriben, con texto propio, los codigos en los que el
 * usuario puede hacer algo concreto.
 */
final class WhatsappMetaErrors {

    private record Rechazo(String code, HttpStatus status, String message) {}

    private static final Map<Integer, Rechazo> CONOCIDOS = Map.ofEntries(
            Map.entry(131058, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "Las plantillas de ejemplo de Meta, como hello_world, solo se pueden enviar "
                            + "desde los numeros de prueba. Crea una plantilla propia, espera su "
                            + "aprobacion y enviala desde tu numero.")),
            Map.entry(131047, new Rechazo(
                    "CRM_WHATSAPP_VENTANA_ATENCION_CERRADA",
                    HttpStatus.BAD_REQUEST,
                    "Pasaron mas de 24 horas desde el ultimo mensaje del cliente. Solo se puede "
                            + "reabrir la conversacion con una plantilla aprobada.")),
            Map.entry(131026, new Rechazo(
                    "CRM_WHATSAPP_DESTINATARIO_NO_DISPONIBLE",
                    HttpStatus.BAD_REQUEST,
                    "El numero del destinatario no puede recibir el mensaje. Verifica que tenga "
                            + "WhatsApp activo y que el codigo de pais sea correcto.")),
            Map.entry(133010, new Rechazo(
                    "CRM_WHATSAPP_DESTINATARIO_NO_DISPONIBLE",
                    HttpStatus.BAD_REQUEST,
                    "El numero emisor no esta registrado en WhatsApp Cloud API. Completa el "
                            + "registro del numero en el Administrador comercial de Meta.")),
            Map.entry(131049, new Rechazo(
                    "CRM_WHATSAPP_ENVIO_LIMITADO_POR_META",
                    HttpStatus.BAD_REQUEST,
                    "Meta decidio no entregar este mensaje de marketing para cuidar la experiencia "
                            + "del usuario. Reintenta mas tarde o usa una plantilla de utilidad.")),
            Map.entry(130472, new Rechazo(
                    "CRM_WHATSAPP_ENVIO_LIMITADO_POR_META",
                    HttpStatus.BAD_REQUEST,
                    "Meta excluyo a este destinatario de los mensajes de marketing. Contactalo por "
                            + "otro canal o espera a que el cliente escriba primero.")),
            Map.entry(130429, new Rechazo(
                    "CRM_WHATSAPP_LIMITE_DE_ENVIOS",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Se alcanzo el limite de envios de la cuenta. Espera unos minutos antes de "
                            + "reintentar.")),
            Map.entry(131048, new Rechazo(
                    "CRM_WHATSAPP_LIMITE_DE_ENVIOS",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Meta bloqueo temporalmente los envios por limite de spam. Revisa la calidad "
                            + "del numero antes de reintentar.")),
            Map.entry(131056, new Rechazo(
                    "CRM_WHATSAPP_LIMITE_DE_ENVIOS",
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados mensajes seguidos a este mismo numero. Espera unos minutos antes "
                            + "de reintentar.")),
            Map.entry(132000, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "La cantidad de parametros enviados no coincide con la plantilla aprobada. "
                            + "Vuelve a sincronizar las plantillas y completa todas las variables.")),
            Map.entry(132001, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "La plantilla no existe o no esta aprobada en ese idioma. Revisa su estado en "
                            + "el Administrador de WhatsApp.")),
            Map.entry(132005, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "El texto resultante supera el limite permitido por la plantilla. Acorta el "
                            + "contenido de las variables.")),
            Map.entry(132007, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "Alguna variable no cumple las reglas de formato de WhatsApp: no puede tener "
                            + "saltos de linea, tabulaciones ni espacios repetidos.")),
            Map.entry(132012, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "El formato de una variable no coincide con el ejemplo aprobado de la "
                            + "plantilla.")),
            Map.entry(132015, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "Meta pauso esta plantilla por baja calidad. Usa otra plantilla o corrigela en "
                            + "el Administrador de WhatsApp.")),
            Map.entry(132016, new Rechazo(
                    "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
                    HttpStatus.BAD_REQUEST,
                    "Meta deshabilito esta plantilla de forma permanente. Crea una nueva y espera "
                            + "su aprobacion.")),
            Map.entry(131031, new Rechazo(
                    "CRM_WHATSAPP_CUENTA_RESTRINGIDA",
                    HttpStatus.BAD_REQUEST,
                    "Meta restringio la cuenta de WhatsApp Business. Revisa el estado de la cuenta "
                            + "en el Administrador comercial."))
    );

    private WhatsappMetaErrors() {
    }

    /**
     * Devuelve la excepcion con texto propio cuando el codigo de Meta es accionable,
     * o {@code null} para que el llamador conserve el enmascaramiento generico.
     */
    static BusinessException accionable(JsonNode body) {
        JsonNode error = body.path("error");
        Rechazo rechazo = CONOCIDOS.get(error.path("code").asInt(-1));
        if (rechazo == null) {
            rechazo = CONOCIDOS.get(error.path("error_subcode").asInt(-1));
        }
        return rechazo == null
                ? null
                : new BusinessException(rechazo.code(), rechazo.message(), rechazo.status());
    }
}
