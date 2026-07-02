package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionDetalleRequest;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.CreateCotizacionRequest;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import com.azurion.saascore.cotizaciones.domain.entities.PromocionCotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.cotizaciones.domain.repositories.PromocionCotizacionRepository;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final PromocionCotizacionRepository promocionRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public CotizacionResponse execute(CreateCotizacionRequest request) {
        authorizationService.validarSucursal(authorizationService.currentUsuarioId(), request.sucursalId());

        Cliente cliente = request.clienteId() == null ? null : clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setUsuarioId(request.usuarioId().trim());
        cotizacion.setUsuarioNombre(request.usuarioNombre().trim());
        cotizacion.setSucursal(sucursal);
        cotizacion.setFechaEmision(request.fechaEmision() == null ? LocalDate.now() : request.fechaEmision());
        cotizacion.setFechaVencimiento(request.fechaVencimiento());
        cotizacion.setMoneda(normalizeMoneda(request.moneda()));
        cotizacion.setObservacion(trim(request.observacion()));
        cotizacion.setCrmOportunidadId(request.crmOportunidadId());
        cotizacion.setEstado("BORRADOR");

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CotizacionDetalleRequest detalleRequest : request.detalles()) {
            CotizacionDetalle detalle = buildDetalle(cotizacion, detalleRequest);
            subtotal = subtotal.add(detalle.getTotal());
            cotizacion.getDetalles().add(detalle);
        }
        cotizacion.setSubtotal(money(subtotal));
        cotizacion.setTotal(money(subtotal));

        return CotizacionMapper.toResponse(cotizacionRepository.save(cotizacion));
    }

    private CotizacionDetalle buildDetalle(Cotizacion cotizacion, CotizacionDetalleRequest request) {
        Producto producto = request.productoId() == null ? null : productoRepository.findById(request.productoId())
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));
        PromocionCotizacion promocion = request.promocionId() == null ? null : promocionRepository.findById(request.promocionId())
                .orElseThrow(() -> new BusinessException("PROMOCION_COTIZACION_NO_ENCONTRADA", "Promocion de cotizacion no encontrada"));
        String descripcion = trim(request.descripcion());
        if (producto == null && descripcion == null) {
            throw new BusinessException("COTIZACION_DETALLE_DESCRIPCION_REQUERIDA",
                    "La cotizacion CRM debe incluir una descripcion del producto u oferta.");
        }
        BigDecimal cantidad = positive(request.cantidad(), "La cantidad debe ser mayor a cero");
        BigDecimal precio = nonNegative(request.precioUnitario(), "El precio no puede ser negativo");
        BigDecimal descuento = nonNegative(request.descuento(), "El descuento no puede ser negativo");
        BigDecimal bruto = money(cantidad.multiply(precio));
        BigDecimal basePromocion = bruto.subtract(descuento);
        if (basePromocion.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("COTIZACION_DETALLE_TOTAL_INVALIDO", "El descuento no puede superar el total del item");
        }
        BigDecimal promocionDescuento = calculatePromotionDiscount(promocion, basePromocion);
        BigDecimal total = money(basePromocion.subtract(promocionDescuento));
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("COTIZACION_DETALLE_TOTAL_INVALIDO", "El descuento no puede superar el total del item");
        }

        CotizacionDetalle detalle = new CotizacionDetalle();
        detalle.setCotizacion(cotizacion);
        detalle.setProducto(producto);
        detalle.setPromocion(promocion);
        detalle.setDescripcion(descripcion);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(money(precio));
        detalle.setDescuento(money(descuento));
        detalle.setPromocionDescuento(promocionDescuento);
        detalle.setTotal(total);
        return detalle;
    }

    private BigDecimal calculatePromotionDiscount(PromocionCotizacion promocion, BigDecimal base) {
        if (promocion == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        validatePromocionActiva(promocion);
        BigDecimal discount;
        if ("PORCENTAJE".equals(promocion.getTipoDescuento())) {
            discount = base.multiply(promocion.getValor()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        } else if ("MONTO".equals(promocion.getTipoDescuento())) {
            discount = promocion.getValor();
        } else {
            throw new BusinessException("PROMOCION_COTIZACION_TIPO_INVALIDO", "Tipo de descuento invalido");
        }
        BigDecimal resolved = money(discount);
        return resolved.compareTo(base) > 0 ? money(base) : resolved;
    }

    private void validatePromocionActiva(PromocionCotizacion promocion) {
        if (!Set.of("ACTIVA").contains(promocion.getEstado())) {
            throw new BusinessException("PROMOCION_COTIZACION_INACTIVA", "La promocion seleccionada no esta activa");
        }
        LocalDate today = LocalDate.now();
        if (promocion.getFechaInicio() != null && promocion.getFechaInicio().isAfter(today)) {
            throw new BusinessException("PROMOCION_COTIZACION_FUERA_DE_FECHA", "La promocion aun no esta vigente");
        }
        if (promocion.getFechaFin() != null && promocion.getFechaFin().isBefore(today)) {
            throw new BusinessException("PROMOCION_COTIZACION_FUERA_DE_FECHA", "La promocion ya vencio");
        }
    }

    private BigDecimal positive(BigDecimal value, String message) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("COTIZACION_VALOR_INVALIDO", message);
        }
        return resolved;
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("COTIZACION_VALOR_INVALIDO", message);
        }
        return resolved;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeMoneda(String value) {
        return value == null || value.isBlank() ? "PEN" : value.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
