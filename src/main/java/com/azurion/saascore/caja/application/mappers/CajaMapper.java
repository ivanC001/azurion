package com.azurion.saascore.caja.application.mappers;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.dto.CajaResponse;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;

public final class CajaMapper {

    private CajaMapper() {
    }

    public static CajaResponse toResponse(Caja caja) {
        return new CajaResponse(
                caja.getId(),
                caja.getSucursal().getId(),
                caja.getSucursal().getCodigo(),
                caja.getSucursal().getNombre(),
                caja.getCodigo(),
                caja.getNombre(),
                caja.getEstado(),
                caja.getSaldoCapital(),
                caja.getSaldoActual(),
                caja.getSaldoSalida(),
                caja.getTotalEntradas(),
                caja.getTotalSalidas(),
                caja.getTotalDepositos(),
                caja.getDiferenciaCierre(),
                caja.getResponsableAperturaId(),
                caja.getResponsableAperturaNombre(),
                caja.getResponsableCierreId(),
                caja.getResponsableCierreNombre(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getObservacionApertura(),
                caja.getObservacionCierre()
        );
    }

    public static CajaMovimientoResponse toMovimientoResponse(CajaMovimiento movimiento) {
        return new CajaMovimientoResponse(
                movimiento.getId(),
                movimiento.getCaja().getId(),
                movimiento.getTipoMovimiento(),
                movimiento.getMonto(),
                movimiento.getSaldoAnterior(),
                movimiento.getSaldoResultante(),
                movimiento.getDescripcion(),
                movimiento.getReferencia(),
                movimiento.getCuentaEmpresarial(),
                movimiento.getResponsableId(),
                movimiento.getResponsableNombre(),
                movimiento.getFechaMovimiento()
        );
    }
}
