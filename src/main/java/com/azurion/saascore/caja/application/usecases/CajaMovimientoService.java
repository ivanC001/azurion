package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.services.CajaActorService;
import com.azurion.saascore.caja.application.services.CajaTurnoService;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.entities.CajaTurno;
import com.azurion.saascore.caja.domain.repositories.CajaMovimientoRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CajaMovimientoService {

    private static final Set<String> MEDIOS_PAGO = Set.of(
            "EFECTIVO",
            "TARJETA",
            "YAPE",
            "PLIN",
            "TRANSFERENCIA",
            "CREDITO"
    );

    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final CajaActorService cajaActorService;
    private final CajaTurnoService cajaTurnoService;

    public CajaMovimiento registrar(CajaTurno turno,
                                    String tipoMovimiento,
                                    String origen,
                                    String medioPago,
                                    BigDecimal monto,
                                    String descripcion,
                                    String referencia,
                                    String cuentaEmpresarial,
                                    Long ventaId,
                                    String clientOperationId,
                                    String requestHash) {
        cajaTurnoService.requireOpen(turno);
        BigDecimal amount = money(monto);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("MONTO_INVALIDO", "El monto debe ser mayor a cero");
        }

        String type = normalizeType(tipoMovimiento);
        String source = normalizeSource(origen);
        String paymentMethod = normalizePaymentMethod(medioPago);
        boolean cashImpact = affectsCash(type, paymentMethod);
        BigDecimal previousBalance = turno.getSaldoEsperado();
        BigDecimal resultingBalance = cashImpact
                ? previousBalance.add(direction(type).multiply(amount))
                : previousBalance;
        if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("SALDO_CAJA_INSUFICIENTE", "La caja no tiene efectivo suficiente");
        }

        updateTotals(turno, type, paymentMethod, amount);
        turno.setSaldoEsperado(resultingBalance);
        CajaActorService.Actor actor = cajaActorService.actual();

        CajaMovimiento movimiento = new CajaMovimiento();
        movimiento.setTurno(turno);
        movimiento.setTipoMovimiento(type);
        movimiento.setOrigen(source);
        movimiento.setMedioPago(paymentMethod);
        movimiento.setAfectaEfectivo(cashImpact);
        movimiento.setVentaId(ventaId);
        movimiento.setMonto(amount);
        movimiento.setSaldoAnterior(previousBalance);
        movimiento.setSaldoResultante(resultingBalance);
        movimiento.setDescripcion(descripcion.trim());
        movimiento.setReferencia(trim(referencia));
        movimiento.setCuentaEmpresarial(trim(cuentaEmpresarial));
        movimiento.setResponsableId(actor.referenciaId());
        movimiento.setResponsableNombre(actor.nombre());
        movimiento.setFechaMovimiento(OffsetDateTime.now());
        movimiento.setAnulado(false);
        movimiento.setClientOperationId(trim(clientOperationId));
        movimiento.setRequestHash(trim(requestHash));
        return movimiento.getClientOperationId() == null
                ? cajaMovimientoRepository.save(movimiento)
                : cajaMovimientoRepository.saveAndFlush(movimiento);
    }

    private void updateTotals(CajaTurno turno, String type, String paymentMethod, BigDecimal amount) {
        switch (type) {
            case "VENTA" -> {
                turno.setNumeroVentas(turno.getNumeroVentas() + 1);
                turno.setTotalVentas(turno.getTotalVentas().add(amount));
                switch (paymentMethod) {
                    case "EFECTIVO" -> turno.setTotalEfectivo(turno.getTotalEfectivo().add(amount));
                    case "TARJETA" -> turno.setTotalTarjeta(turno.getTotalTarjeta().add(amount));
                    case "YAPE", "PLIN" ->
                            turno.setTotalBilleteraDigital(turno.getTotalBilleteraDigital().add(amount));
                    case "TRANSFERENCIA" ->
                            turno.setTotalTransferencia(turno.getTotalTransferencia().add(amount));
                    case "CREDITO" -> turno.setTotalCredito(turno.getTotalCredito().add(amount));
                    default -> throw new BusinessException("MEDIO_PAGO_INVALIDO", "Medio de pago no soportado");
                }
            }
            case "INGRESO" ->
                    turno.setTotalIngresosManuales(turno.getTotalIngresosManuales().add(amount));
            case "RETIRO" -> turno.setTotalRetiros(turno.getTotalRetiros().add(amount));
            case "DEPOSITO" -> turno.setTotalDepositos(turno.getTotalDepositos().add(amount));
            case "REEMBOLSO" -> turno.setTotalReembolsos(turno.getTotalReembolsos().add(amount));
            default -> throw new BusinessException("TIPO_MOVIMIENTO_CAJA_INVALIDO", "Tipo de movimiento no soportado");
        }
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("TIPO_MOVIMIENTO_CAJA_INVALIDO", "Selecciona un tipo de movimiento");
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ENTRADA", "INGRESO" -> "INGRESO";
            case "SALIDA", "RETIRO" -> "RETIRO";
            case "DEPOSITO_CUENTA", "DEPOSITO" -> "DEPOSITO";
            case "VENTA" -> "VENTA";
            case "REEMBOLSO" -> "REEMBOLSO";
            default -> throw new BusinessException(
                    "TIPO_MOVIMIENTO_CAJA_INVALIDO",
                    "Use INGRESO, RETIRO, DEPOSITO o REEMBOLSO"
            );
        };
    }

    private String normalizeSource(String value) {
        return value == null || value.isBlank() ? "MANUAL" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePaymentMethod(String value) {
        String normalized = value == null || value.isBlank()
                ? "EFECTIVO"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!MEDIOS_PAGO.contains(normalized)) {
            throw new BusinessException(
                    "MEDIO_PAGO_INVALIDO",
                    "Use EFECTIVO, TARJETA, YAPE, PLIN, TRANSFERENCIA o CREDITO"
            );
        }
        return normalized;
    }

    private boolean affectsCash(String type, String paymentMethod) {
        return !"VENTA".equals(type) || "EFECTIVO".equals(paymentMethod);
    }

    private BigDecimal direction(String type) {
        return switch (type) {
            case "VENTA", "INGRESO" -> BigDecimal.ONE;
            case "RETIRO", "DEPOSITO", "REEMBOLSO" -> BigDecimal.ONE.negate();
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new BusinessException("MONTO_INVALIDO", "El monto es obligatorio");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
