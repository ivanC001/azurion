package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.repositories.CajaMovimientoRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CajaMovimientoService {

    private static final String ABIERTA = "ABIERTA";

    private final CajaMovimientoRepository cajaMovimientoRepository;

    public CajaMovimiento registrar(Caja caja,
                                    String tipoMovimiento,
                                    BigDecimal monto,
                                    String descripcion,
                                    String referencia,
                                    String cuentaEmpresarial,
                                    String responsableId,
                                    String responsableNombre) {
        if (!ABIERTA.equals(caja.getEstado())) {
            throw new BusinessException("CAJA_NO_ABIERTA", "La caja debe estar abierta para registrar movimientos");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("MONTO_INVALIDO", "El monto debe ser mayor a cero");
        }

        BigDecimal saldoAnterior = caja.getSaldoActual();
        BigDecimal saldoResultante = saldoAnterior;
        String tipo = tipoMovimiento.trim().toUpperCase();

        switch (tipo) {
            case "ENTRADA" -> {
                saldoResultante = saldoAnterior.add(monto);
                caja.setTotalEntradas(caja.getTotalEntradas().add(monto));
            }
            case "SALIDA" -> {
                saldoResultante = saldoAnterior.subtract(monto);
                validarSaldoNoNegativo(saldoResultante);
                caja.setTotalSalidas(caja.getTotalSalidas().add(monto));
            }
            case "DEPOSITO_CUENTA" -> {
                saldoResultante = saldoAnterior.subtract(monto);
                validarSaldoNoNegativo(saldoResultante);
                caja.setTotalDepositos(caja.getTotalDepositos().add(monto));
            }
            default -> throw new BusinessException("TIPO_MOVIMIENTO_CAJA_INVALIDO", "Use ENTRADA, SALIDA o DEPOSITO_CUENTA");
        }

        caja.setSaldoActual(saldoResultante);

        CajaMovimiento movimiento = new CajaMovimiento();
        movimiento.setCaja(caja);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoResultante(saldoResultante);
        movimiento.setDescripcion(descripcion);
        movimiento.setReferencia(referencia);
        movimiento.setCuentaEmpresarial(cuentaEmpresarial);
        movimiento.setResponsableId(responsableId);
        movimiento.setResponsableNombre(responsableNombre);
        movimiento.setFechaMovimiento(OffsetDateTime.now());

        return cajaMovimientoRepository.save(movimiento);
    }

    private void validarSaldoNoNegativo(BigDecimal saldoResultante) {
        if (saldoResultante.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("SALDO_CAJA_INSUFICIENTE", "La caja no tiene saldo suficiente");
        }
    }
}
