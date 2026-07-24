package com.azurion.saascore.empresas.application.dto;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EmpresaOperationalSummaryResponse(
        EmpresaResponse empresa,
        SuscripcionResponse suscripcion,
        boolean suscripcionVigente,
        BigDecimal precioMensual,
        Long limiteMensualBolsa,
        Long usuariosTotal,
        Long usuariosActivos,
        Long usuariosInactivos,
        Integer cuposDisponibles,
        boolean cupoExcedido,
        boolean conteoUsuariosDisponible,
        List<String> moduloCodigos,
        LocalDateTime creadaEn,
        LocalDateTime actualizadaEn
) {
}
