package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaResponse;
import com.azurion.saascore.caja.application.dto.TipoComprobanteVenta;
import com.azurion.saascore.caja.application.usecases.RegistrarVentaCajaUseCase;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.application.dto.ConvertCotizacionVentaRequest;
import com.azurion.saascore.cotizaciones.application.dto.ConvertCotizacionVentaResponse;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.application.services.CrmCotizacionIntegrationService;
import com.azurion.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConvertCotizacionVentaUseCase {

    private final GetCotizacionUseCase getCotizacionUseCase;
    private final CotizacionRepository cotizacionRepository;
    private final RegistrarVentaCajaUseCase registrarVentaCajaUseCase;
    private final CrmCotizacionIntegrationService crmCotizacionIntegrationService;

    @Transactional
    public ConvertCotizacionVentaResponse execute(Long id, ConvertCotizacionVentaRequest request) {
        Cotizacion cotizacion = getCotizacionUseCase.find(id);
        if (cotizacion.getVentaId() != null || "CONVERTIDA".equals(cotizacion.getEstado())) {
            throw new BusinessException("COTIZACION_YA_CONVERTIDA", "La cotizacion ya fue convertida en venta");
        }
        if (!"ACEPTADA".equals(cotizacion.getEstado())) {
            throw new BusinessException("COTIZACION_NO_ACEPTADA", "Solo una cotizacion aceptada puede convertirse en venta");
        }

        RegistrarVentaCajaResponse venta = registrarVentaCajaUseCase.execute(request.cajaId(), buildVentaRequest(cotizacion, request));
        cotizacion.setEstado("CONVERTIDA");
        cotizacion.setVentaId(venta.venta().id());
        cotizacion.setConvertidaEn(OffsetDateTime.now());
        Cotizacion saved = cotizacionRepository.save(cotizacion);
        crmCotizacionIntegrationService.onCotizacionConvertidaVenta(saved.getCrmOportunidadId(), saved.getTotal());

        return new ConvertCotizacionVentaResponse(CotizacionMapper.toResponse(saved), venta);
    }

    private RegistrarVentaCajaRequest buildVentaRequest(Cotizacion cotizacion, ConvertCotizacionVentaRequest request) {
        Cliente cliente = cotizacion.getCliente();
        TipoComprobanteVenta tipo = request.tipoComprobante() == null ? TipoComprobanteVenta.TICKET_VENTA : request.tipoComprobante();
        return new RegistrarVentaCajaRequest(
                tipo,
                cotizacion.getTotal(),
                request.responsableId(),
                request.responsableNombre(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getTipoDocumento(),
                cliente == null ? null : cliente.getNumeroDocumento(),
                cliente == null ? null : cliente.getNombre(),
                request.fechaEmision(),
                request.moneda() == null || request.moneda().isBlank() ? cotizacion.getMoneda() : request.moneda(),
                request.tipoCambio(),
                request.formaPago(),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                "Venta generada desde cotizacion #" + cotizacion.getId(),
                buildItems(cotizacion.getDetalles())
        );
    }

    private List<RegistrarVentaCajaRequest.VentaProductoRequest> buildItems(List<CotizacionDetalle> detalles) {
        return detalles.stream()
                .map(detalle -> {
                    if (detalle.getProducto() == null || detalle.getProducto().getAlmacen() == null) {
                        throw new BusinessException(
                                "COTIZACION_SIN_PRODUCTO_ERP",
                                "Solo se puede convertir a venta una cotizacion con productos ERP e inventario vinculados"
                        );
                    }
                    return new RegistrarVentaCajaRequest.VentaProductoRequest(
                        detalle.getProducto().getId(),
                        detalle.getProducto().getAlmacen().getId(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getDescuento(),
                        null,
                        detalle.getDescripcion(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    );
                })
                .toList();
    }
}
