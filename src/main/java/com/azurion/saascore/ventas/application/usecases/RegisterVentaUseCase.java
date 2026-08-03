package com.azurion.saascore.ventas.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.ventas.application.dto.RegisterVentaRequest;
import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.entities.VentaDetalle;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.saascore.ventas.domain.repositories.VentaDetalleRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.shared.contracts.ventas.SaleRegisteredEvent;
import com.azurion.shared.event.InternalEventBus;
import com.azurion.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterVentaUseCase {

    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final ProductoRepository productoRepository;
    private final InternalEventBus eventBus;

    @Transactional
    public VentaResponse execute(RegisterVentaRequest request) {
        return execute(request, Venta.FACTURACION_ESTADO_PENDIENTE, null, null);
    }

    @Transactional
    public VentaResponse execute(RegisterVentaRequest request, String facturacionEstadoInicial) {
        return execute(request, facturacionEstadoInicial, null, null);
    }

    @Transactional
    public VentaResponse execute(
            RegisterVentaRequest request,
            String facturacionEstadoInicial,
            String clientOperationId,
            String requestHash
    ) {
        ventaRepository.findByExternalId(request.externalId()).ifPresent(existing -> {
            throw new BusinessException("VENTA_DUPLICATED", "A sale with same externalId already exists");
        });

        Venta venta = new Venta();
        venta.setExternalId(request.externalId());
        venta.setClienteDocumento(request.clienteDocumento());
        venta.setClienteNombre(request.clienteNombre());
        venta.setMoneda(request.moneda());
        venta.setTotal(request.total());
        venta.setCajaTurnoId(request.cajaTurnoId());
        venta.setFormaPago(request.formaPago());
        venta.setMetodoPago(request.metodoPago());
        venta.setFechaVenta(OffsetDateTime.now());
        venta.setFacturacionEstado(facturacionEstadoInicial);
        venta.setFacturacionIntentos(0);
        venta.setFacturacionActualizadoEn(OffsetDateTime.now());
        venta.setClientOperationId(clientOperationId);
        venta.setRequestHash(requestHash);

        Venta saved = clientOperationId == null
                ? ventaRepository.save(venta)
                : ventaRepository.saveAndFlush(venta);
        request.items().forEach(item -> {
            VentaDetalle detalle = new VentaDetalle();
            detalle.setVenta(saved);
            if (item.productoId() != null) {
                var producto = productoRepository.findById(item.productoId())
                        .orElseThrow(() -> new BusinessException(
                                "PRODUCTO_NO_ENCONTRADO",
                                "Producto no encontrado: " + item.productoId()
                        ));
                detalle.setProducto(producto);
                BigDecimal costo = producto.getCostoPromedio();
                if (costo == null || costo.compareTo(BigDecimal.ZERO) <= 0) {
                    costo = producto.getPrecioCompraBase();
                }
                detalle.setCostoUnitarioInventariable(
                        (costo == null ? BigDecimal.ZERO : costo).setScale(6, RoundingMode.HALF_UP)
                );
            } else {
                detalle.setCostoUnitarioInventariable(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            }
            detalle.setSku(item.sku());
            detalle.setDescripcion(item.description());
            detalle.setCantidad(item.quantity());
            detalle.setPrecioUnitario(item.unitPrice());
            detalle.setDescuento(item.discount());
            detalle.setTipoOperacionCodigo(item.tipoOperacionCodigo());
            detalle.setTipoAfectacionIgvCodigo(item.tipoAfectacionIgvCodigo());
            detalle.setTributoCodigo(item.tributoCodigo());
            detalle.setPorcentajeIgv(item.porcentajeIgv());
            detalle.setBaseImponible(item.baseImponible());
            detalle.setMontoIgv(item.montoIgv());
            detalle.setTotal(item.lineTotal());
            ventaDetalleRepository.save(detalle);
        });

        eventBus.publish(new SaleRegisteredEvent(
                TenantContext.getTenantId(),
                saved.getExternalId(),
                saved.getClienteDocumento(),
                saved.getClienteNombre(),
                saved.getMoneda(),
                saved.getTotal(),
                request.items().stream()
                        .map(i -> new SaleRegisteredEvent.SaleItem(i.sku(), i.description(), i.quantity(), i.unitPrice(), i.lineTotal()))
                        .collect(Collectors.toList()),
                OffsetDateTime.now()
        ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<CompletedVentaOperation> findCompletedOperation(String clientOperationId) {
        if (clientOperationId == null || clientOperationId.isBlank()) {
            return Optional.empty();
        }
        return ventaRepository.findByClientOperationId(clientOperationId.trim())
                .map(venta -> new CompletedVentaOperation(toResponse(venta), venta.getRequestHash()));
    }

    private VentaResponse toResponse(Venta saved) {
        return new VentaResponse(
                saved.getId(),
                saved.getExternalId(),
                saved.getClienteDocumento(),
                saved.getClienteNombre(),
                saved.getMoneda(),
                saved.getTotal(),
                saved.getCajaTurnoId(),
                saved.getFormaPago(),
                saved.getMetodoPago(),
                saved.getFechaVenta(),
                saved.getFacturacionEstado(),
                saved.getFacturacionIntentos(),
                saved.getFacturadorHttpStatus(),
                saved.getFacturadorEndpoint(),
                saved.getFacturadorTipoComprobante(),
                saved.getFacturadorMensaje(),
                saved.getFacturadorSunatEstado(),
                saved.getFacturadorDocumentoId(),
                saved.getFacturadorTicket(),
                saved.getFacturadorPdfUrl(),
                saved.getFacturadorXmlUrl(),
                saved.getFacturadorCdrUrl(),
                saved.getFacturadorRespuestaJson(),
                saved.getFacturacionActualizadoEn()
        );
    }

    public record CompletedVentaOperation(VentaResponse venta, String requestHash) {
    }
}
