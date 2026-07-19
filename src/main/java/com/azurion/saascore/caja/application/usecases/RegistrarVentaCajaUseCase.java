package com.azurion.saascore.caja.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.caja.application.dto.FacturadorVentaResponse;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaResponse;
import com.azurion.saascore.caja.application.dto.TipoComprobanteVenta;
import com.azurion.saascore.caja.application.dto.VentaFacturacionAsyncTask;
import com.azurion.saascore.caja.application.mappers.CajaMapper;
import com.azurion.saascore.caja.application.services.VentaSucursalStockPolicy;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.saascore.caja.domain.entities.CajaMovimiento;
import com.azurion.saascore.caja.domain.repositories.CajaRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.application.usecases.StockMovimientoUseCase;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.tributacion.application.dto.TaxResolution;
import com.azurion.saascore.tributacion.application.services.TaxResolverService;
import com.azurion.saascore.tributacion.domain.entities.ConfiguracionTributariaEmpresa;
import com.azurion.saascore.ventas.application.dto.RegisterVentaRequest;
import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.application.usecases.RegisterVentaUseCase;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarVentaCajaUseCase {
    private static final Set<String> AFECTACION_GRAVADA = Set.of(
            "10", "11", "12", "13", "14", "15", "16", "17"
    );
    private static final Set<String> AFECTACION_GRATUITA = Set.of(
            "11", "12", "13", "14", "15", "16", "17", "21", "31", "32", "33", "34", "35", "36"
    );

    private final CajaRepository cajaRepository;
    private final CajaMovimientoService cajaMovimientoService;
    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final StockMovimientoUseCase stockMovimientoUseCase;
    private final RegisterVentaUseCase registerVentaUseCase;
    private final DispatchVentaFacturacionAsyncUseCase dispatchVentaFacturacionAsyncUseCase;
    private final AuthorizationService authorizationService;
    private final VentaSucursalStockPolicy ventaSucursalStockPolicy;
    private final TaxResolverService taxResolverService;

    @Transactional
    public RegistrarVentaCajaResponse execute(Long cajaId, RegistrarVentaCajaRequest request) {
        authorizationService.validarCaja(authorizationService.currentUsuarioId(), cajaId);
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new BusinessException("CAJA_NO_ENCONTRADA", "Caja no encontrada"));

        String tenantId = TenantContext.getTenantId();
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NO_ENCONTRADO", "Empresa no encontrada para tenant: " + tenantId));
        LocalDate fechaEmision = resolveFechaEmisionDate(request);
        ResolvedVentaCliente clienteVenta = resolveVentaCliente(request);
        validateFacturaCliente(request, clienteVenta);

        List<ResolvedVentaItem> resolvedItems = resolveVentaItems(request, caja);
        ventaSucursalStockPolicy.validar(caja, resolvedItems.stream().map(ResolvedVentaItem::almacenId).toList());
        BigDecimal totalCalculado = resolvedItems.stream()
                .map(ResolvedVentaItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalCalculado.compareTo(request.total()) != 0) {
            throw new BusinessException("TOTAL_VENTA_INVALIDO", "El total de la venta no coincide con el detalle de productos");
        }
        applyClienteCredito(request, clienteVenta);

        for (ResolvedVentaItem item : resolvedItems) {
            stockMovimientoUseCase.execute(new StockMovimientoRequest(
                    item.producto().getId(),
                    item.almacenId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    "SALIDA",
                    "VENTA_CAJA",
                    item.cantidad(),
                    null,
                    null,
                    null,
                    null,
                    item.referencia()
            ));
        }

        boolean requiereFacturador = request.tipoComprobante() != TipoComprobanteVenta.TICKET_VENTA;
        VentaResponse venta = registerVentaUseCase.execute(
                buildVentaRequest(request, resolvedItems, clienteVenta),
                requiereFacturador ? Venta.FACTURACION_ESTADO_PENDIENTE : Venta.FACTURACION_ESTADO_NO_REQUIERE
        );

        FacturadorTarget target = requiereFacturador ? resolveTarget(request.tipoComprobante()) : null;
        if (requiereFacturador) {
            Map<String, Object> payload = buildPayload(request, empresa, caja, target.tipoSunat(), resolvedItems, venta.externalId(), clienteVenta, fechaEmision);
            dispatchVentaFacturacionAsyncUseCase.dispatch(new VentaFacturacionAsyncTask(
                    tenantId,
                    empresa.getRuc(),
                    venta.id(),
                    venta.externalId(),
                    target.endpoint(),
                    request.tipoComprobante().name(),
                    payload
            ));
        }

        String referencia = buildReferenciaPendiente(request.tipoComprobante(), venta.externalId());
        String descripcion = buildMovimientoDescripcion(request, clienteVenta);

        CajaMovimiento movimiento = null;
        if (!"CREDITO".equalsIgnoreCase(safeTrim(request.formaPago()))) {
            movimiento = cajaMovimientoService.registrar(
                    caja,
                    "ENTRADA",
                    request.total(),
                    descripcion,
                    referencia,
                    null,
                    request.responsableId(),
                    request.responsableNombre()
            );
            cajaRepository.save(caja);
        }

        return new RegistrarVentaCajaResponse(
                venta,
                movimiento == null ? null : CajaMapper.toMovimientoResponse(movimiento),
                requiereFacturador
                        ? new FacturadorVentaResponse(
                                true,
                                202,
                                target.endpoint(),
                                request.tipoComprobante().name(),
                                "Venta registrada. Facturacion en cola para procesamiento.",
                                Map.of("estado", "PENDIENTE", "external_id", venta.externalId())
                        )
                        : new FacturadorVentaResponse(
                                true,
                                200,
                                "",
                                request.tipoComprobante().name(),
                                "Ticket interno registrado. No requiere Facturador ni SUNAT.",
                                Map.of("estado", "NO_REQUIERE", "external_id", venta.externalId())
                        )
        );
    }

    private FacturadorTarget resolveTarget(TipoComprobanteVenta tipo) {
        return switch (tipo) {
            case FACTURA -> new FacturadorTarget("/facturas", "01");
            case BOLETA, BOLETA_SIN_NOMBRE -> new FacturadorTarget("/boletas", "03");
            case TICKET_VENTA -> throw new BusinessException("TICKET_INTERNO", "El ticket interno no se envia al facturador");
        };
    }

    private Map<String, Object> buildPayload(RegistrarVentaCajaRequest request,
                                             Empresa empresa,
                                             Caja caja,
                                             String tipoSunat,
                                             List<ResolvedVentaItem> items,
                                             String externalId,
                                             ResolvedVentaCliente clienteVenta,
                                             LocalDate fechaEmision) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("empresa", Map.of(
                "ruc", empresa.getRuc(),
                "razon_social", empresa.getRazonSocial()
        ));

        payload.put("cliente", buildCliente(request, clienteVenta));

        BigDecimal mtoOperGravadas = BigDecimal.ZERO;
        BigDecimal mtoOperExoneradas = BigDecimal.ZERO;
        BigDecimal mtoOperInafectas = BigDecimal.ZERO;
        BigDecimal mtoOperExportacion = BigDecimal.ZERO;
        BigDecimal mtoOperGratuitas = BigDecimal.ZERO;
        BigDecimal mtoIgv = BigDecimal.ZERO;
        BigDecimal mtoIgvGratuitas = BigDecimal.ZERO;
        BigDecimal mtoIsc = BigDecimal.ZERO;
        BigDecimal mtoOtrosTributos = BigDecimal.ZERO;
        BigDecimal mtoIcbper = BigDecimal.ZERO;

        List<Map<String, Object>> detalles = new ArrayList<>();
        for (ResolvedVentaItem item : items) {
            String afectacion = item.afectacionIgv();
            boolean gratuita = isAfectacionGratuita(afectacion);
            BigDecimal lineTotal = scaleMoney(item.lineTotal());
            BigDecimal lineBase = item.baseImponible();
            BigDecimal porcentajeIgv = item.porcentajeIgv();
            BigDecimal lineIgv = item.montoIgv();
            BigDecimal valueUnit = calculateValueUnit(lineBase, item.cantidad());
            BigDecimal priceUnit = gratuita ? BigDecimal.ZERO : calculatePriceUnit(lineTotal, item.cantidad());
            BigDecimal lineIsc = scaleMoney(item.isc());
            BigDecimal lineOtroTributo = scaleMoney(item.otroTributo());
            BigDecimal lineIcbper = scaleMoney(item.icbper());
            BigDecimal lineTotalImpuestos = lineIgv.add(lineIsc).add(lineOtroTributo).add(lineIcbper);

            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("sku", item.producto().getSku());
            detalle.put("codigo", item.producto().getSku());
            detalle.put("descripcion", item.descripcion());
            detalle.put("unidad", item.unidad());
            detalle.put("cantidad", item.cantidad());
            detalle.put("precio_unitario", item.precioUnitario());
            detalle.put("valor_unitario", valueUnit);
            detalle.put("mto_valor_venta", lineBase);
            detalle.put("igv", lineIgv);
            detalle.put("porcentaje_igv", porcentajeIgv);
            detalle.put("tip_afe_igv", afectacion);
            detalle.put("tributo_codigo", item.tributoCodigo());
            detalle.put("descuento", scaleMoney(item.descuento()));
            detalle.put("total", gratuita ? BigDecimal.ZERO : lineTotal);
            detalle.put("mto_valor_unitario", valueUnit);
            detalle.put("mto_precio_unitario", priceUnit);
            detalle.put("total_impuestos", lineTotalImpuestos);

            if (item.codigoSunat() != null && !item.codigoSunat().isBlank()) {
                detalle.put("codigo_sunat", item.codigoSunat());
            }
            if (gratuita) {
                detalle.put("mto_valor_gratuito", scaleMoney(item.mtoValorGratuito() != null ? item.mtoValorGratuito() : valueUnit));
                detalle.put("precio_unitario", BigDecimal.ZERO);
                detalle.put("valor_unitario", BigDecimal.ZERO);
            }
            if (lineIcbper.compareTo(BigDecimal.ZERO) > 0) {
                detalle.put("icbper", lineIcbper);
                detalle.put("factor_icbper", scaleMoney(item.factorIcbper()));
            }
            if (lineIsc.compareTo(BigDecimal.ZERO) > 0) {
                detalle.put("isc", lineIsc);
                detalle.put("porcentaje_isc", scaleMoney(item.porcentajeIsc()));
                detalle.put("tip_sis_isc", item.tipSisIsc());
            }
            if (lineOtroTributo.compareTo(BigDecimal.ZERO) > 0) {
                detalle.put("otro_tributo", lineOtroTributo);
                detalle.put("porcentaje_oth", scaleMoney(item.porcentajeOtroTributo()));
            }
            if (!item.descuentos().isEmpty()) {
                detalle.put("descuentos", item.descuentos());
            }
            if (!item.cargos().isEmpty()) {
                detalle.put("cargos", item.cargos());
            }
            detalles.add(detalle);

            if (gratuita) {
                mtoOperGratuitas = mtoOperGratuitas.add(lineBase);
                mtoIgvGratuitas = mtoIgvGratuitas.add(lineIgv);
            } else if ("40".equals(afectacion)) {
                mtoOperExportacion = mtoOperExportacion.add(lineBase);
            } else if ("20".equals(afectacion) || "21".equals(afectacion)) {
                mtoOperExoneradas = mtoOperExoneradas.add(lineBase);
            } else if (Set.of("30", "31", "32", "33", "34", "35", "36").contains(afectacion)) {
                mtoOperInafectas = mtoOperInafectas.add(lineBase);
            } else {
                mtoOperGravadas = mtoOperGravadas.add(lineBase);
            }

            mtoIgv = mtoIgv.add(gratuita ? BigDecimal.ZERO : lineIgv);
            mtoIsc = mtoIsc.add(lineIsc);
            mtoOtrosTributos = mtoOtrosTributos.add(lineOtroTributo);
            mtoIcbper = mtoIcbper.add(lineIcbper);
        }
        payload.put("detalles", detalles);

        BigDecimal valorVenta = mtoOperGravadas
                .add(mtoOperExoneradas)
                .add(mtoOperInafectas)
                .add(mtoOperExportacion);

        BigDecimal totalImpuestos = mtoIgv.add(mtoIsc).add(mtoOtrosTributos).add(mtoIcbper);
        BigDecimal subTotal = valorVenta.add(totalImpuestos);
        BigDecimal totalDocumento = scaleMoney(request.total());

        Map<String, Object> documento = new LinkedHashMap<>();
        documento.put("tipo", tipoSunat);
        documento.put("fecha_emision", fechaEmision.toString());
        documento.put("moneda", items.getFirst().moneda());
        documento.put("tipo_cambio", request.tipoCambio() == null ? new BigDecimal("3.80") : request.tipoCambio());
        documento.put("total", totalDocumento);
        documento.put("external_id", externalId);
        documento.put("tipo_operacion", items.getFirst().tipoOperacionCodigo());
        documento.put("observacion", request.descripcion());
        documento.put("mto_oper_gravadas", scaleMoney(mtoOperGravadas));
        documento.put("mto_oper_exoneradas", scaleMoney(mtoOperExoneradas));
        documento.put("mto_oper_inafectas", scaleMoney(mtoOperInafectas));
        documento.put("mto_oper_exportacion", scaleMoney(mtoOperExportacion));
        documento.put("mto_oper_gratuitas", scaleMoney(mtoOperGratuitas));
        documento.put("mto_igv_gratuitas", scaleMoney(mtoIgvGratuitas));
        documento.put("igv_total", scaleMoney(mtoIgv));
        documento.put("mto_isc", scaleMoney(mtoIsc));
        documento.put("mto_otros_tributos", scaleMoney(mtoOtrosTributos));
        documento.put("mto_icbper", scaleMoney(mtoIcbper));
        documento.put("total_impuestos", scaleMoney(totalImpuestos));
        documento.put("valor_venta", scaleMoney(valorVenta));
        documento.put("sub_total", scaleMoney(subTotal));
        documento.put("forma_pago", resolveFormaPago(request, totalDocumento));
        documento.put("contingencia", Boolean.TRUE.equals(request.contingencia()));

        if (request.percepcion() != null) {
            Map<String, Object> percepcion = new LinkedHashMap<>();
            percepcion.put("codigo_regimen", request.percepcion().codigoRegimen());
            percepcion.put("porcentaje", scaleMoney(request.percepcion().porcentaje()));
            percepcion.put("monto_base", scaleMoney(request.percepcion().montoBase()));
            percepcion.put("monto", scaleMoney(request.percepcion().monto()));
            percepcion.put("monto_total", scaleMoney(request.percepcion().montoTotal()));
            documento.put("percepcion", percepcion);
        }

        if (request.detraccion() != null) {
            Map<String, Object> detraccion = new LinkedHashMap<>();
            detraccion.put("cod_bien_detraccion", request.detraccion().codigoBien());
            detraccion.put("cod_medio_pago", request.detraccion().codigoMedioPago());
            detraccion.put("cta_banco", request.detraccion().cuentaBanco());
            detraccion.put("porcentaje", scaleMoney(request.detraccion().porcentaje()));
            detraccion.put("monto", scaleMoney(request.detraccion().monto()));
            detraccion.put("valor_referencial", scaleMoney(request.detraccion().valorReferencial()));
            documento.put("detraccion", detraccion);
        }

        if (request.anticipos() != null && !request.anticipos().isEmpty()) {
            List<Map<String, Object>> anticipos = request.anticipos().stream()
                    .map(anticipo -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("tipo_doc_rel", anticipo.tipoDocRel());
                        row.put("nro_doc_rel", anticipo.nroDocRel());
                        row.put("total", scaleMoney(anticipo.total()));
                        return row;
                    })
                    .toList();
            documento.put("anticipos", anticipos);
            BigDecimal totalAnticipos = request.anticipos().stream()
                    .map(anticipo -> anticipo.total() == null ? BigDecimal.ZERO : anticipo.total())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            documento.put("total_anticipos", scaleMoney(totalAnticipos));
        }

        if (request.leyendas() != null && !request.leyendas().isEmpty()) {
            List<Map<String, Object>> leyendas = request.leyendas().stream()
                    .map(leyenda -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("codigo", leyenda.codigo());
                        row.put("valor", leyenda.valor());
                        return row;
                    })
                    .toList();
            documento.put("leyendas", leyendas);
        }
        payload.put("documento", documento);

        if (caja.getSucursal() != null && caja.getSucursal().getCodigo() != null) {
            payload.put("sucursal", Map.of("codigo", caja.getSucursal().getCodigo()));
        }

        return payload;
    }

    private Map<String, Object> buildCliente(RegistrarVentaCajaRequest request, ResolvedVentaCliente clienteVenta) {
        if (request.tipoComprobante() == TipoComprobanteVenta.TICKET_VENTA) {
            return Map.of();
        }

        if (request.tipoComprobante() == TipoComprobanteVenta.BOLETA_SIN_NOMBRE) {
            return Map.of(
                    "tipo_doc", "0",
                    "num_doc", "-",
                    "razon_social", "CLIENTES VARIOS"
            );
        }

        String nombre = clienteVenta != null ? clienteVenta.nombre() : safeTrim(request.clienteNombre());
        String numeroDoc = clienteVenta != null ? clienteVenta.numeroDocumento() : safeTrim(request.clienteNumeroDocumento());
        String tipoDoc = clienteVenta != null ? clienteVenta.tipoDocumento() : safeTrim(request.clienteTipoDocumento());

        if (request.tipoComprobante() == TipoComprobanteVenta.FACTURA) {
            if (!"6".equals(tipoDoc) || !numeroDoc.matches("^[0-9]{11}$") || nombre.isBlank()) {
                throw new BusinessException(
                        "CLIENTE_FACTURA_INVALIDO",
                        "La factura solo puede emitirse a un cliente con RUC de 11 digitos y razon social"
                );
            }
            return buildClienteData("6", numeroDoc, nombre, clienteVenta);
        }

        if (nombre.isBlank() && numeroDoc.isBlank()) {
            return Map.of(
                    "tipo_doc", "0",
                    "num_doc", "-",
                    "razon_social", "CLIENTES VARIOS"
            );
        }

        if (tipoDoc.isBlank()) {
            tipoDoc = numeroDoc.length() == 8 ? "1" : "0";
        }

        return buildClienteData(
                tipoDoc,
                numeroDoc.isBlank() ? "-" : numeroDoc,
                nombre.isBlank() ? "CLIENTE" : nombre,
                clienteVenta
        );
    }

    private Map<String, Object> buildClienteData(String tipoDoc,
                                                 String numeroDoc,
                                                 String nombre,
                                                 ResolvedVentaCliente clienteVenta) {
        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("tipo_doc", tipoDoc);
        cliente.put("num_doc", numeroDoc);
        cliente.put("razon_social", nombre);
        if (clienteVenta == null) {
            return cliente;
        }
        putIfPresent(cliente, "direccion", clienteVenta.direccion());
        putIfPresent(cliente, "ubigeo", clienteVenta.ubigeo());
        putIfPresent(cliente, "email", clienteVenta.email());
        putIfPresent(cliente, "telefono", clienteVenta.telefono());
        return cliente;
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        String normalized = safeTrim(value);
        if (!normalized.isBlank()) {
            target.put(key, normalized);
        }
    }

    private String buildReferenciaPendiente(TipoComprobanteVenta tipo, String externalId) {
        return switch (tipo) {
            case FACTURA -> "FAC-PEND-" + externalId;
            case BOLETA, BOLETA_SIN_NOMBRE -> "BOL-PEND-" + externalId;
            case TICKET_VENTA -> "TKT-PEND-" + externalId;
        };
    }

    private String buildMovimientoDescripcion(RegistrarVentaCajaRequest request, ResolvedVentaCliente clienteVenta) {
        StringBuilder description = new StringBuilder("VENTA ");
        description.append(request.tipoComprobante().name());
        String clienteNombre = clienteVenta != null ? clienteVenta.nombre() : safeTrim(request.clienteNombre());
        if (!clienteNombre.isBlank()) {
            description.append(" | Cliente: ").append(clienteNombre);
        }
        if (request.descripcion() != null && !request.descripcion().isBlank()) {
            description.append(" | ").append(request.descripcion().trim());
        }
        return description.toString();
    }

    private String resolveMoneda(String moneda) {
        String normalized = safeTrim(moneda);
        return normalized.isBlank() ? "PEN" : normalized.toUpperCase();
    }

    private LocalDate resolveFechaEmisionDate(RegistrarVentaCajaRequest request) {
        ZoneId zoneId = ZoneId.of("America/Lima");
        LocalDate today = LocalDate.now(zoneId);
        LocalDate minDate = today.minusDays(2);
        LocalDate issueDate = parseDateOrToday(request.fechaEmision(), today);

        if (request.tipoComprobante() == TipoComprobanteVenta.FACTURA
                && (issueDate.isBefore(minDate) || issueDate.isAfter(today))) {
            throw new BusinessException(
                    "FECHA_FACTURA_INVALIDA",
                    "La fecha de emision de factura debe estar entre "
                            + minDate
                            + " y "
                            + today
            );
        }

        return issueDate;
    }

    private LocalDate parseDateOrToday(String rawDate, LocalDate fallback) {
        String raw = safeTrim(rawDate);
        if (raw.isBlank()) {
            return fallback;
        }
        try {
            if (raw.length() >= 10) {
                return LocalDate.parse(raw.substring(0, 10));
            }
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private ResolvedVentaCliente resolveVentaCliente(RegistrarVentaCajaRequest request) {
        if (request.tipoComprobante() == TipoComprobanteVenta.TICKET_VENTA
                || request.tipoComprobante() == TipoComprobanteVenta.BOLETA_SIN_NOMBRE) {
            return null;
        }

        if (request.clienteId() != null) {
            Cliente byId = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado: " + request.clienteId()));
            return toResolvedVentaCliente(byId);
        }

        String numeroDocumento = safeTrim(request.clienteNumeroDocumento());
        if (numeroDocumento.isBlank()) {
            return null;
        }

        String tipoDocumento = request.tipoComprobante() == TipoComprobanteVenta.FACTURA
                ? "6"
                : inferCustomerDocumentType(safeTrim(request.clienteTipoDocumento()), numeroDocumento);
        if ("1".equals(tipoDocumento) && !numeroDocumento.matches("^[0-9]{8}$")) {
            return null;
        }
        if ("6".equals(tipoDocumento) && !numeroDocumento.matches("^[0-9]{11}$")) {
            return null;
        }

        Cliente existing = clienteRepository.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento).orElse(null);
        if (existing != null) {
            return toResolvedVentaCliente(existing);
        }

        String nombre = safeTrim(request.clienteNombre());
        if (nombre.isBlank()) {
            return null;
        }

        Cliente created = new Cliente();
        created.setTipoDocumento(tipoDocumento);
        created.setNumeroDocumento(numeroDocumento);
        created.setNombre(nombre);
        created.setEmail(null);
        Cliente saved = clienteRepository.save(created);

        return toResolvedVentaCliente(saved);
    }

    private ResolvedVentaCliente toResolvedVentaCliente(Cliente cliente) {
        return new ResolvedVentaCliente(
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getUbigeo(),
                cliente.getTelefono()
        );
    }

    private void validateFacturaCliente(RegistrarVentaCajaRequest request, ResolvedVentaCliente clienteVenta) {
        if (request.tipoComprobante() != TipoComprobanteVenta.FACTURA) {
            return;
        }
        if (clienteVenta == null
                || !"6".equals(clienteVenta.tipoDocumento())
                || !clienteVenta.numeroDocumento().matches("^[0-9]{11}$")
                || clienteVenta.nombre().isBlank()) {
            throw new BusinessException(
                    "CLIENTE_FACTURA_INVALIDO",
                    "La factura solo puede emitirse a un cliente con RUC de 11 digitos y razon social"
            );
        }
    }

    private String inferCustomerDocumentType(String providedType, String documentNumber) {
        if ("1".equals(providedType) || "6".equals(providedType)) {
            return providedType;
        }
        if (documentNumber.length() == 11) {
            return "6";
        }
        return "1";
    }

    private void applyClienteCredito(RegistrarVentaCajaRequest request, ResolvedVentaCliente clienteVenta) {
        if (!"CREDITO".equalsIgnoreCase(safeTrim(request.formaPago()))) {
            return;
        }
        if (clienteVenta == null || clienteVenta.id() == null) {
            throw new BusinessException("CLIENTE_CREDITO_REQUERIDO", "Una venta a credito requiere un cliente registrado");
        }

        Cliente cliente = clienteRepository.findById(clienteVenta.id())
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));
        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new BusinessException("CLIENTE_INACTIVO", "El cliente esta inactivo y no puede comprar a credito");
        }

        BigDecimal limite = cliente.getLimiteCredito() == null ? BigDecimal.ZERO : cliente.getLimiteCredito();
        BigDecimal deudaActual = cliente.getSaldoDeuda() == null ? BigDecimal.ZERO : cliente.getSaldoDeuda();
        BigDecimal nuevaDeuda = deudaActual.add(request.total());

        if (limite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("CLIENTE_SIN_CREDITO", "El cliente no tiene limite de credito asignado");
        }
        if (nuevaDeuda.compareTo(limite) > 0) {
            throw new BusinessException(
                    "LIMITE_CREDITO_EXCEDIDO",
                    "La venta excede el credito disponible del cliente"
            );
        }

        cliente.setSaldoDeuda(nuevaDeuda);
        clienteRepository.save(cliente);
    }

    private Map<String, Object> resolveFormaPago(RegistrarVentaCajaRequest request, BigDecimal totalDocumento) {
        String tipo = safeTrim(request.formaPago()).toUpperCase();
        if (tipo.isBlank()) {
            tipo = "CONTADO";
        }

        Map<String, Object> formaPago = new LinkedHashMap<>();
        formaPago.put("tipo", tipo);

        if ("CREDITO".equals(tipo)) {
            formaPago.put("monto", scaleMoney(totalDocumento));

            if (request.cuotas() != null && !request.cuotas().isEmpty()) {
                List<Map<String, Object>> cuotas = request.cuotas().stream()
                        .map(cuota -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("monto", scaleMoney(cuota.monto()));
                            row.put("fecha_pago", cuota.fechaPago());
                            row.put("moneda", safeTrim(cuota.moneda()).isBlank() ? resolveMoneda(request.moneda()) : cuota.moneda());
                            return row;
                        })
                        .toList();
                formaPago.put("cuotas", cuotas);
            }
        }

        return formaPago;
    }

    static BigDecimal calculateLineBase(BigDecimal lineTotal, String afectacion, BigDecimal porcentajeIgv) {
        if (!isAfectacionGravada(afectacion)) {
            return scaleMoney(lineTotal);
        }
        BigDecimal factor = BigDecimal.ONE.add(porcentajeIgv.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return scaleMoney(lineTotal.divide(factor, 2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateLineBaseGratuita(ResolvedVentaItem item) {
        if (item.mtoValorGratuito() != null && item.mtoValorGratuito().compareTo(BigDecimal.ZERO) > 0) {
            return scaleMoney(item.mtoValorGratuito().multiply(item.cantidad()));
        }
        BigDecimal gross = item.precioUnitario().multiply(item.cantidad());
        if (gross.compareTo(BigDecimal.ZERO) > 0) {
            return scaleMoney(gross);
        }
        return scaleMoney(item.lineTotal());
    }

    static BigDecimal calculateLineIgv(BigDecimal lineTotal, BigDecimal lineBase, String afectacion, BigDecimal porcentajeIgv) {
        if (!isAfectacionIgvCalculable(afectacion)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (!isAfectacionGratuita(afectacion)) {
            return scaleMoney(lineTotal.subtract(lineBase));
        }
        return scaleMoney(lineBase.multiply(porcentajeIgv).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateValueUnit(BigDecimal lineBase, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return lineBase.divide(quantity, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePriceUnit(BigDecimal lineTotal, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scaleMoney(lineTotal.divide(quantity, 2, RoundingMode.HALF_UP));
    }

    private static boolean isAfectacionGravada(String afectacionIgv) {
        return AFECTACION_GRAVADA.contains(afectacionIgv);
    }

    private static boolean isAfectacionGratuita(String afectacionIgv) {
        return AFECTACION_GRATUITA.contains(afectacionIgv);
    }

    private static boolean isAfectacionIgvCalculable(String afectacionIgv) {
        return isAfectacionGravada(afectacionIgv) || isAfectacionGratuita(afectacionIgv);
    }

    private static BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<ResolvedVentaItem> resolveVentaItems(RegistrarVentaCajaRequest request, Caja caja) {
        List<ResolvedVentaItem> resolved = new ArrayList<>();
        ConfiguracionTributariaEmpresa taxEmpresa = taxResolverService.configuracionEmpresa();
        for (RegistrarVentaCajaRequest.VentaProductoRequest item : request.items()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado: " + item.productoId()));
            if (!producto.isActivo()) {
                throw new BusinessException("PRODUCTO_INACTIVO", "Producto inactivo: " + producto.getSku());
            }

            Long almacenId = item.almacenId() != null ? item.almacenId() : producto.getAlmacen().getId();
            BigDecimal grossLineTotal = item.precioUnitario().multiply(item.cantidad());
            BigDecimal descuento = item.descuento() == null ? BigDecimal.ZERO : item.descuento();
            if (descuento.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("DESCUENTO_INVALIDO", "El descuento no puede ser negativo");
            }
            if (descuento.compareTo(grossLineTotal) > 0) {
                throw new BusinessException("DESCUENTO_INVALIDO", "El descuento no puede superar el total de la linea");
            }

            BigDecimal lineTotal = grossLineTotal.subtract(descuento);
            String description = safeTrim(item.descripcion()).isBlank() ? producto.getNombre() : item.descripcion().trim();
            String reference = "VENTA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            TaxResolution tax = taxResolverService.resolverImpuesto(producto, caja.getSucursal(), taxEmpresa);
            String afectacionIgv = tax.tipoAfectacionCodigo();
            BigDecimal porcentajeIgv = scaleMoney(tax.porcentajeIgv());
            BigDecimal baseImponible = calculateLineBase(lineTotal, afectacionIgv, porcentajeIgv);
            BigDecimal montoIgv = calculateLineIgv(lineTotal, baseImponible, afectacionIgv, porcentajeIgv);
            String unidad = safeTrim(item.unidad()).isBlank() ? "NIU" : item.unidad().trim().toUpperCase();
            String codigoSunat = safeTrim(item.codigoSunat());

            resolved.add(new ResolvedVentaItem(
                    producto,
                    almacenId,
                    item.cantidad(),
                    item.precioUnitario(),
                    descuento,
                    lineTotal,
                    afectacionIgv,
                    description,
                    reference,
                    porcentajeIgv,
                    tax.tipoOperacionCodigo(),
                    tax.tributoCodigo(),
                    tax.moneda(),
                    baseImponible,
                    montoIgv,
                    item.mtoValorGratuito(),
                    item.icbper(),
                    item.factorIcbper(),
                    item.isc(),
                    item.porcentajeIsc(),
                    safeTrim(item.tipSisIsc()).isBlank() ? "01" : item.tipSisIsc().trim(),
                    item.otroTributo(),
                    item.porcentajeOtroTributo(),
                    unidad,
                    codigoSunat.isBlank() ? null : codigoSunat,
                    item.descuentos() == null ? List.of() : item.descuentos(),
                    item.cargos() == null ? List.of() : item.cargos()
            ));
        }
        return resolved;
    }

    private RegisterVentaRequest buildVentaRequest(RegistrarVentaCajaRequest request, List<ResolvedVentaItem> items, ResolvedVentaCliente clienteVenta) {
        String externalId = "VENTA-CAJA-"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(OffsetDateTime.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clienteDocumento = clienteVenta != null ? clienteVenta.numeroDocumento() : safeTrim(request.clienteNumeroDocumento());
        String clienteNombre = clienteVenta != null ? clienteVenta.nombre() : safeTrim(request.clienteNombre());

        if (request.tipoComprobante() == TipoComprobanteVenta.BOLETA_SIN_NOMBRE || request.tipoComprobante() == TipoComprobanteVenta.TICKET_VENTA) {
            clienteDocumento = clienteDocumento.isBlank() ? "-" : clienteDocumento;
            clienteNombre = clienteNombre.isBlank() ? "CLIENTES VARIOS" : clienteNombre;
        }

        if (clienteDocumento.isBlank()) {
            clienteDocumento = "-";
        }
        if (clienteNombre.isBlank()) {
            clienteNombre = "CLIENTE";
        }

        List<RegisterVentaRequest.VentaItemRequest> ventaItems = items.stream()
                .map(item -> new RegisterVentaRequest.VentaItemRequest(
                        item.producto().getId(),
                        item.producto().getSku(),
                        item.descripcion(),
                        item.cantidad(),
                        item.precioUnitario(),
                        item.descuento(),
                        item.tipoOperacionCodigo(),
                        item.afectacionIgv(),
                        item.tributoCodigo(),
                        item.porcentajeIgv(),
                        item.baseImponible(),
                        item.montoIgv(),
                        item.lineTotal()
                ))
                .toList();

        return new RegisterVentaRequest(
                externalId,
                clienteDocumento,
                clienteNombre,
                items.getFirst().moneda(),
                request.total(),
                ventaItems
        );
    }

    private record ResolvedVentaItem(
            Producto producto,
            Long almacenId,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal lineTotal,
            String afectacionIgv,
            String descripcion,
            String referencia,
            BigDecimal porcentajeIgv,
            String tipoOperacionCodigo,
            String tributoCodigo,
            String moneda,
            BigDecimal baseImponible,
            BigDecimal montoIgv,
            BigDecimal mtoValorGratuito,
            BigDecimal icbper,
            BigDecimal factorIcbper,
            BigDecimal isc,
            BigDecimal porcentajeIsc,
            String tipSisIsc,
            BigDecimal otroTributo,
            BigDecimal porcentajeOtroTributo,
            String unidad,
            String codigoSunat,
            List<Map<String, Object>> descuentos,
            List<Map<String, Object>> cargos
    ) {
    }

    private record ResolvedVentaCliente(
            Long id,
            String tipoDocumento,
            String numeroDocumento,
            String nombre,
            String email,
            String direccion,
            String ubigeo,
            String telefono
    ) {
    }

    private record FacturadorTarget(String endpoint, String tipoSunat) {
    }
}
