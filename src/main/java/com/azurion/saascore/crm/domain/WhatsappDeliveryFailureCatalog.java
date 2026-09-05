package com.azurion.saascore.crm.domain;

import java.util.Map;
import java.util.Optional;

/**
 * Traduce los codigos con que Meta reporta una entrega fallida.
 *
 * <p>Un envio puede devolver 200 y fallar igual: Meta acepta la llamada, devuelve el
 * wamid y recien segundos despues avisa por webhook que no lo entrego. Ese aviso llega
 * en ingles y con jerga de la plataforma, asi que sin traduccion el usuario ve un
 * mensaje marcado como fallido y ninguna pista de que tiene que hacer.
 *
 * <p>Es distinto de la traduccion que se hace al rechazar un envio en el momento: alli
 * hay que decidir el status HTTP de la excepcion, aca solo explicarle a una persona
 * que paso y como se arregla.
 */
public final class WhatsappDeliveryFailureCatalog {

    /**
     * @param causa    que salio mal, en una linea
     * @param solucion el siguiente paso concreto, o null si no lo hay
     */
    public record Explicacion(String causa, String solucion) {}

    private static final Map<String, Explicacion> CODIGOS = Map.ofEntries(
            Map.entry("131042", new Explicacion(
                    "La cuenta de WhatsApp Business no tiene metodo de pago.",
                    "Cargalo en el Administrador comercial de Meta, en Facturacion. Los envios de "
                            + "plantilla se cobran, asi que sin metodo de pago Meta los rechaza.")),
            Map.entry("131026", new Explicacion(
                    "El numero del destinatario no puede recibir el mensaje.",
                    "Verifica que tenga WhatsApp activo y que el codigo de pais sea el correcto.")),
            Map.entry("131047", new Explicacion(
                    "Pasaron mas de 24 horas desde el ultimo mensaje del cliente.",
                    "Fuera de esa ventana solo se puede reabrir la conversacion con una plantilla "
                            + "aprobada.")),
            Map.entry("131049", new Explicacion(
                    "Meta decidio no entregar el mensaje de marketing para cuidar la experiencia "
                            + "del usuario.",
                    "Reintenta mas tarde o usa una plantilla de utilidad.")),
            Map.entry("130472", new Explicacion(
                    "Meta excluyo a este destinatario de los mensajes de marketing.",
                    "Contactalo por otro canal o espera a que el cliente escriba primero.")),
            Map.entry("131048", new Explicacion(
                    "Meta bloqueo temporalmente los envios por limite de spam.",
                    "Revisa la calidad del numero en el Administrador de WhatsApp antes de "
                            + "reintentar.")),
            Map.entry("130429", new Explicacion(
                    "Se alcanzo el limite de envios de la cuenta.",
                    "Espera unos minutos antes de reintentar.")),
            Map.entry("131056", new Explicacion(
                    "Demasiados mensajes seguidos al mismo numero.",
                    "Espera unos minutos antes de volver a escribirle.")),
            Map.entry("131031", new Explicacion(
                    "Meta restringio la cuenta de WhatsApp Business.",
                    "Revisa el estado de la cuenta en el Administrador comercial.")),
            Map.entry("132015", new Explicacion(
                    "Meta pauso la plantilla por baja calidad.",
                    "Usa otra plantilla o corregila en el Administrador de WhatsApp.")),
            Map.entry("132016", new Explicacion(
                    "Meta deshabilito la plantilla de forma permanente.",
                    "Crea una nueva y espera su aprobacion.")),
            Map.entry("131058", new Explicacion(
                    "Las plantillas de ejemplo de Meta solo salen desde sus numeros de prueba.",
                    "Crea una plantilla propia y espera su aprobacion.")),
            Map.entry("133010", new Explicacion(
                    "El numero emisor no esta registrado en WhatsApp Cloud API.",
                    "Completa el registro del numero en el Administrador comercial."))
    );

    private WhatsappDeliveryFailureCatalog() {
    }

    public static Optional<Explicacion> explicar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(CODIGOS.get(codigo.trim()));
    }
}
