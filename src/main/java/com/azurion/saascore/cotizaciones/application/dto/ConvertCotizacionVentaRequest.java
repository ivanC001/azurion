package com.azurion.saascore.cotizaciones.application.dto;

import com.azurion.saascore.caja.application.dto.TipoComprobanteVenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConvertCotizacionVentaRequest(
        @NotNull Long cajaId,
        TipoComprobanteVenta tipoComprobante,
        @NotBlank String responsableId,
        @NotBlank String responsableNombre,
        String formaPago,
        String fechaEmision,
        String moneda,
        @DecimalMin("0.00") BigDecimal tipoCambio
) {
}
