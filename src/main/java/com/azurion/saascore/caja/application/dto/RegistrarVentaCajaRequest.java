package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RegistrarVentaCajaRequest(
        @NotNull TipoComprobanteVenta tipoComprobante,
        @NotNull @DecimalMin("0.01") BigDecimal total,
        @NotBlank String responsableId,
        @NotBlank String responsableNombre,
        Long clienteId,
        String clienteTipoDocumento,
        String clienteNumeroDocumento,
        String clienteNombre,
        String fechaEmision,
        String moneda,
        BigDecimal tipoCambio,
        String formaPago,
        Boolean contingencia,
        String tipoOperacionSunat,
        @Valid PercepcionRequest percepcion,
        @Valid DetraccionRequest detraccion,
        List<@Valid AnticipoRequest> anticipos,
        List<@Valid CuotaRequest> cuotas,
        List<@Valid LeyendaRequest> leyendas,
        String descripcion,
        @NotEmpty List<@Valid VentaProductoRequest> items
) {
    public record PercepcionRequest(
            String codigoRegimen,
            @DecimalMin("0.00") BigDecimal porcentaje,
            @DecimalMin("0.00") BigDecimal montoBase,
            @DecimalMin("0.00") BigDecimal monto,
            @DecimalMin("0.00") BigDecimal montoTotal
    ) {
    }

    public record DetraccionRequest(
            String codigoBien,
            String codigoMedioPago,
            String cuentaBanco,
            @DecimalMin("0.00") BigDecimal porcentaje,
            @DecimalMin("0.00") BigDecimal monto,
            @DecimalMin("0.00") BigDecimal valorReferencial
    ) {
    }

    public record AnticipoRequest(
            String tipoDocRel,
            String nroDocRel,
            @DecimalMin("0.00") BigDecimal total
    ) {
    }

    public record CuotaRequest(
            @DecimalMin("0.00") BigDecimal monto,
            String fechaPago,
            String moneda
    ) {
    }

    public record LeyendaRequest(
            String codigo,
            String valor
    ) {
    }

    public record VentaProductoRequest(
            @NotNull Long productoId,
            Long almacenId,
            @NotNull @Positive BigDecimal cantidad,
            @NotNull @DecimalMin("0.01") BigDecimal precioUnitario,
            @DecimalMin("0.00") BigDecimal descuento,
            String afectacionIgv,
            String descripcion,
            String codigoSunat,
            String unidad,
            @DecimalMin("0.00") BigDecimal porcentajeIgv,
            @DecimalMin("0.00") BigDecimal mtoValorGratuito,
            @DecimalMin("0.00") BigDecimal icbper,
            @DecimalMin("0.00") BigDecimal factorIcbper,
            @DecimalMin("0.00") BigDecimal isc,
            @DecimalMin("0.00") BigDecimal porcentajeIsc,
            String tipSisIsc,
            @DecimalMin("0.00") BigDecimal otroTributo,
            @DecimalMin("0.00") BigDecimal porcentajeOtroTributo,
            List<Map<String, Object>> descuentos,
            List<Map<String, Object>> cargos
    ) {
    }
}
