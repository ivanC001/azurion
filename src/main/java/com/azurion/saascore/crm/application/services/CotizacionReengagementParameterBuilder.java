package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CotizacionReengagementField;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Arma los valores de las variables de una plantilla a partir de una cotizacion.
 *
 * <p>Meta rechaza los parametros que traen saltos de linea, tabulaciones o espacios
 * repetidos (error 132007), asi que todo sale colapsado a una sola linea.
 */
@Component
public class CotizacionReengagementParameterBuilder {

    /** Los cinco campos de la plantilla de seguimiento de cotizacion. */
    public static final List<CotizacionReengagementField> CAMPOS_POR_DEFECTO = List.of(
            CotizacionReengagementField.NOMBRE,
            CotizacionReengagementField.COTIZACION,
            CotizacionReengagementField.PRODUCTO,
            CotizacionReengagementField.TOTAL,
            CotizacionReengagementField.VENCIMIENTO
    );

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> SIMBOLOS = Map.of(
            "PEN", "S/",
            "USD", "$",
            "EUR", "€"
    );
    private static final int MAX_LARGO = 1024;

    public List<String> build(
            CrmProspecto prospecto,
            Cotizacion cotizacion,
            List<CotizacionReengagementField> campos) {
        List<CotizacionReengagementField> solicitados =
                campos == null || campos.isEmpty() ? CAMPOS_POR_DEFECTO : campos;
        return solicitados.stream()
                .map(campo -> valor(campo, prospecto, cotizacion))
                .toList();
    }

    private String valor(
            CotizacionReengagementField campo,
            CrmProspecto prospecto,
            Cotizacion cotizacion) {
        return switch (campo) {
            case NOMBRE -> limpiar(primerNombre(prospecto.getNombre()), campo);
            case NOMBRE_COMPLETO -> limpiar(prospecto.getNombre(), campo);
            case COTIZACION -> limpiar(String.valueOf(cotizacion.getId()), campo);
            case PRODUCTO -> limpiar(producto(prospecto, cotizacion), campo);
            case TOTAL -> limpiar(total(cotizacion), campo);
            case VENCIMIENTO -> limpiar(vencimiento(cotizacion), campo);
            case ASESOR -> limpiar(asesor(cotizacion), campo);
        };
    }

    private String primerNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return nombre.trim().split("\\s+")[0];
    }

    /** Lo cotizado: el primer item de la cotizacion, o el interes del prospecto. */
    private String producto(CrmProspecto prospecto, Cotizacion cotizacion) {
        for (CotizacionDetalle detalle : cotizacion.getDetalles()) {
            String nombre = primerNoVacio(
                    detalle.getCatalogoNombre(),
                    detalle.getDescripcion(),
                    detalle.getProducto() == null ? null : detalle.getProducto().getNombre()
            );
            if (nombre != null) {
                return nombre;
            }
        }
        return prospecto.getInteresPrincipal();
    }

    private String total(Cotizacion cotizacion) {
        BigDecimal total = cotizacion.getTotal();
        if (total == null) {
            return null;
        }
        String moneda = cotizacion.getMoneda() == null ? "" : cotizacion.getMoneda();
        String simbolo = SIMBOLOS.getOrDefault(moneda, moneda);
        return (simbolo + " " + total.setScale(2, RoundingMode.HALF_UP).toPlainString()).trim();
    }

    private String vencimiento(Cotizacion cotizacion) {
        return cotizacion.getFechaVencimiento() == null
                ? null
                : cotizacion.getFechaVencimiento().format(FECHA);
    }

    private String asesor(Cotizacion cotizacion) {
        return primerNoVacio(
                unir(cotizacion.getUsuarioNombre(), cotizacion.getAsesorApellidos()),
                cotizacion.getUsuarioNombre()
        );
    }

    private String unir(String primero, String segundo) {
        if (primero == null || primero.isBlank()) {
            return segundo;
        }
        return segundo == null || segundo.isBlank() ? primero : primero + " " + segundo;
    }

    private String primerNoVacio(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    /**
     * Deja el valor en una sola linea. Un campo vacio se rechaza aqui y no en Meta:
     * asi el error dice cual falta en vez de un 132000 generico una semana despues.
     */
    private String limpiar(String valor, CotizacionReengagementField campo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(
                    "CRM_WHATSAPP_REENGANCHE_DATO_FALTANTE",
                    "La cotizacion no tiene con que llenar el campo " + campo.name()
                            + ". Completalo en la cotizacion o quitalo de la plantilla."
            );
        }
        String normalizado = valor.replaceAll("\\s+", " ").trim();
        return normalizado.length() <= MAX_LARGO ? normalizado : normalizado.substring(0, MAX_LARGO);
    }
}
