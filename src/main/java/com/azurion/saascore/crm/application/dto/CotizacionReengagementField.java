package com.azurion.saascore.crm.application.dto;

/**
 * Valores que el CRM puede sacar de una cotizacion para llenar las variables de una
 * plantilla de reenganche, sin que nadie los escriba a mano.
 *
 * <p>El orden en que se piden es el orden en que se mandan: la variable {{1}} de la
 * plantilla recibe el primer campo de la lista, {{2}} el segundo, y asi.
 */
public enum CotizacionReengagementField {

    /** Primer nombre del prospecto, para el saludo. */
    NOMBRE,

    /** Nombre completo tal como esta registrado. */
    NOMBRE_COMPLETO,

    /** Identificador de la cotizacion, para que el cliente sepa de cual se habla. */
    COTIZACION,

    /** Que se cotizo: el primer item, o el interes principal del prospecto. */
    PRODUCTO,

    /** Total con su moneda, ya formateado. */
    TOTAL,

    /** Fecha de vencimiento en formato dd/MM/yyyy. */
    VENCIMIENTO,

    /** Asesor que la emitio. */
    ASESOR
}
