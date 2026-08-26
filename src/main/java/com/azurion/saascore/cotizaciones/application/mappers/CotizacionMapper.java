package com.azurion.saascore.cotizaciones.application.mappers;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionDetalleResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import java.util.List;

public final class CotizacionMapper {

    private CotizacionMapper() {
    }

    public static CotizacionResponse toResponse(Cotizacion cotizacion) {
        Cliente cliente = cotizacion.getCliente();
        return new CotizacionResponse(
                cotizacion.getId(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getNumeroDocumento(),
                cliente == null ? null : cliente.getNombre(),
                cotizacion.getUsuarioId(),
                cotizacion.getUsuarioNombre(),
                cotizacion.getAsesorApellidos(),
                cotizacion.getAsesorTelefono(),
                cotizacion.getAsesorEmail(),
                cotizacion.getAsesorCargo(),
                cotizacion.getAsesorFotoUrl(),
                cotizacion.getSucursal().getId(),
                cotizacion.getSucursal().getCodigo(),
                cotizacion.getSucursal().getNombre(),
                cotizacion.getFechaEmision(),
                cotizacion.getFechaVencimiento(),
                cotizacion.getMoneda(),
                cotizacion.getMonedaBase(),
                cotizacion.getTipoCambioAplicado(),
                cotizacion.getFechaTipoCambio(),
                cotizacion.getSubtotal(),
                cotizacion.getTotal(),
                cotizacion.getSubtotalMonedaBase(),
                cotizacion.getTotalMonedaBase(),
                cotizacion.getEstado(),
                cotizacion.getObservacion(),
                cotizacion.getVentaId(),
                cotizacion.getCrmOportunidadId(),
                cotizacion.getFechaEnvio(),
                cotizacion.getCanalEnvio(),
                cotizacion.getProximoSeguimientoEn(),
                cotizacion.getFechaRespuesta(),
                cotizacion.getMotivoRechazo(),
                cotizacion.getDecisionSiguiente(),
                cotizacion.getConvertidaEn(),
                cotizacion.getWhatsappSendStatus(),
                cotizacion.getWhatsappMessageId(),
                cotizacion.getDetalles().stream().map(CotizacionMapper::toDetalleResponse).toList()
        );
    }

    private static CotizacionDetalleResponse toDetalleResponse(CotizacionDetalle detalle) {
        return new CotizacionDetalleResponse(
                detalle.getId(),
                detalle.getProducto() == null ? null : detalle.getProducto().getId(),
                detalle.getProducto() == null ? null : detalle.getProducto().getSku(),
                detalle.getProducto() == null ? null : detalle.getProducto().getNombre(),
                detalle.getCatalogoItemId(),
                detalle.getCatalogoTipoItem(),
                detalle.getCatalogoNombre(),
                detalle.getCatalogoDescripcion(),
                detalle.getCatalogoMetadataJson(),
                detalle.getCatalogoMoneda(),
                detalle.getCatalogoPrecioReferencial(),
                detalle.getPromocion() == null ? null : detalle.getPromocion().getId(),
                detalle.getPromocion() == null ? null : detalle.getPromocion().getNombre(),
                detalle.getDescripcion(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getDescuento(),
                detalle.getPromocionDescuento(),
                detalle.getTotal()
        );
    }

    public static List<CotizacionResponse> toResponses(List<Cotizacion> cotizaciones) {
        return cotizaciones.stream().map(CotizacionMapper::toResponse).toList();
    }
}
