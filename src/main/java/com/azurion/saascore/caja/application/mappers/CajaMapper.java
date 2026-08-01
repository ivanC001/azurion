package com.azurion.saascore.caja.application.mappers;

import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.dto.CajaFisicaResponse;
import com.azurion.saascore.caja.application.dto.CajaTurnoResponse;
import com.azurion.saascore.caja.domain.entities.CajaFisica;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.entities.CajaTurno;
import java.util.List;

public final class CajaMapper {

    private CajaMapper() {
    }

    public static CajaFisicaResponse toFisicaResponse(CajaFisica caja, List<Long> usuarioIds) {
        return new CajaFisicaResponse(
                caja.getId(),
                caja.getSucursal().getId(),
                caja.getSucursal().getCodigo(),
                caja.getSucursal().getNombre(),
                caja.getCodigo(),
                caja.getNombre(),
                caja.getMoneda(),
                caja.getEstado(),
                usuarioIds == null ? List.of() : List.copyOf(usuarioIds)
        );
    }

    public static CajaTurnoResponse toTurnoResponse(CajaTurno turno) {
        CajaFisica caja = turno.getCaja();
        return new CajaTurnoResponse(
                turno.getId(),
                turno.getNumero(),
                caja.getId(),
                caja.getSucursal().getId(),
                caja.getSucursal().getCodigo(),
                caja.getSucursal().getNombre(),
                caja.getCodigo(),
                caja.getNombre(),
                turno.getMoneda(),
                turno.getEstado(),
                turno.getUsuarioId(),
                turno.getSaldoApertura(),
                turno.getSaldoEsperado(),
                turno.getConteoFisico(),
                turno.getDiferenciaCierre(),
                turno.getNumeroVentas(),
                turno.getTotalVentas(),
                turno.getTotalEfectivo(),
                turno.getTotalTarjeta(),
                turno.getTotalBilleteraDigital(),
                turno.getTotalTransferencia(),
                turno.getTotalCredito(),
                turno.getTotalIngresosManuales(),
                turno.getTotalRetiros(),
                turno.getTotalDepositos(),
                turno.getTotalReembolsos(),
                turno.getResponsableAperturaId(),
                turno.getResponsableAperturaNombre(),
                turno.getResponsableCierreId(),
                turno.getResponsableCierreNombre(),
                turno.getFechaApertura(),
                turno.getFechaCierre(),
                turno.getObservacionApertura(),
                turno.getObservacionCierre()
        );
    }

    public static CajaMovimientoResponse toMovimientoResponse(CajaMovimiento movimiento) {
        return new CajaMovimientoResponse(
                movimiento.getId(),
                movimiento.getTurno().getId(),
                movimiento.getTurno().getCaja().getId(),
                movimiento.getTipoMovimiento(),
                movimiento.getOrigen(),
                movimiento.getMedioPago(),
                movimiento.isAfectaEfectivo(),
                movimiento.getVentaId(),
                movimiento.getMonto(),
                movimiento.getSaldoAnterior(),
                movimiento.getSaldoResultante(),
                movimiento.getDescripcion(),
                movimiento.getReferencia(),
                movimiento.getCuentaEmpresarial(),
                movimiento.getResponsableId(),
                movimiento.getResponsableNombre(),
                movimiento.getFechaMovimiento(),
                movimiento.isAnulado(),
                movimiento.getMotivoAnulacion()
        );
    }
}
