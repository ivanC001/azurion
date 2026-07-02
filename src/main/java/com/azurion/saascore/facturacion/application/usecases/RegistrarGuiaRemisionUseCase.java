package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.caja.application.dto.FacturadorVentaResponse;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.application.dto.GuiaRemisionResponse;
import com.azurion.saascore.facturacion.application.dto.RegistrarGuiaRemisionRequest;
import com.azurion.saascore.facturacion.application.dto.RegistrarGuiaRemisionResponse;
import com.azurion.saascore.facturacion.domain.entities.GuiaRemision;
import com.azurion.saascore.facturacion.domain.repositories.GuiaRemisionRepository;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrarGuiaRemisionUseCase {

    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final GuiaRemisionRepository guiaRemisionRepository;
    private final FacturadorClient facturadorClient;

    public RegistrarGuiaRemisionResponse execute(RegistrarGuiaRemisionRequest request) {
        if (request.sucursalOrigenId().equals(request.sucursalDestinoId())) {
            throw new BusinessException("GUIA_SUCURSAL_DESTINO_INVALIDA", "La sucursal destino debe ser distinta a origen.");
        }

        String tenantId = TenantContext.getTenantId();
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NO_ENCONTRADO", "Empresa no encontrada para tenant: " + tenantId));

        Sucursal origen = sucursalRepository.findById(request.sucursalOrigenId())
                .orElseThrow(() -> new BusinessException("GUIA_SUCURSAL_ORIGEN_NO_ENCONTRADA", "Sucursal origen no encontrada."));
        Sucursal destino = sucursalRepository.findById(request.sucursalDestinoId())
                .orElseThrow(() -> new BusinessException("GUIA_SUCURSAL_DESTINO_NO_ENCONTRADA", "Sucursal destino no encontrada."));

        if (!origen.isActivo() || !destino.isActivo()) {
            throw new BusinessException("GUIA_SUCURSAL_INACTIVA", "Origen y destino deben ser sucursales activas.");
        }

        LocalDate fechaTraslado = parseFechaTraslado(request.fechaTraslado());
        LocalDate fechaEmision = LocalDate.now(ZoneId.of("America/Lima"));
        String externalId = "GUIA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        List<ResolvedGuiaItem> items = resolveItems(request.items());

        GuiaRemision guia = createGuia(
                request,
                origen,
                destino,
                items,
                externalId,
                fechaEmision,
                fechaTraslado
        );

        Map<String, Object> payload = buildPayload(
                request,
                empresa,
                origen,
                destino,
                items,
                externalId,
                fechaEmision,
                fechaTraslado
        );

        markGuiaProcessing(guia);

        try {
            FacturadorClient.FacturadorEmissionResult emission = facturadorClient.emitirDocumento(
                    tenantId,
                    empresa.getRuc(),
                    "/guias",
                    payload,
                    "GUIA_REMISION"
            );

            updateGuiaFromEmission(guia, emission);

            return new RegistrarGuiaRemisionResponse(
                    externalId,
                    toResponse(guia),
                    new FacturadorVentaResponse(
                            emission.success(),
                            emission.status(),
                            emission.endpoint(),
                            emission.tipoComprobante(),
                            emission.message(),
                            emission.responseBody()
                    )
            );
        } catch (BusinessException exception) {
            markGuiaError(guia, exception.getMessage());
            return new RegistrarGuiaRemisionResponse(
                    externalId,
                    toResponse(guia),
                    new FacturadorVentaResponse(
                            false,
                            500,
                            "/guias",
                            "GUIA_REMISION",
                            exception.getMessage(),
                            null
                    )
            );
        } catch (Exception exception) {
            markGuiaError(guia, exception.getMessage());
            String message = "No se pudo enviar guia al facturador: " + exception.getMessage();
            return new RegistrarGuiaRemisionResponse(
                    externalId,
                    toResponse(guia),
                    new FacturadorVentaResponse(
                            false,
                            500,
                            "/guias",
                            "GUIA_REMISION",
                            message,
                            null
                    )
            );
        }
    }

    private GuiaRemision createGuia(
            RegistrarGuiaRemisionRequest request,
            Sucursal origen,
            Sucursal destino,
            List<ResolvedGuiaItem> items,
            String externalId,
            LocalDate fechaEmision,
            LocalDate fechaTraslado
    ) {
        GuiaRemision guia = new GuiaRemision();
        guia.setExternalId(externalId);
        guia.setSucursalOrigenId(origen.getId());
        guia.setSucursalOrigenNombre(trimToMax(origen.getNombre(), 255));
        guia.setSucursalDestinoId(destino.getId());
        guia.setSucursalDestinoNombre(trimToMax(destino.getNombre(), 255));
        guia.setFechaEmision(fechaEmision);
        guia.setFechaTraslado(fechaTraslado);
        guia.setMotivoTraslado(trimToMax(defaultIfBlank(trim(request.motivoTraslado()), "VENTA"), 120));
        guia.setTransportista(trimToMax(trim(request.transportista()), 255));
        guia.setObservacion(trimToMax(trim(request.observacion()), 500));
        guia.setResponsableId(trimToMax(defaultIfBlank(trim(request.responsableId()), "system"), 120));
        guia.setResponsableNombre(trimToMax(defaultIfBlank(trim(request.responsableNombre()), "Usuario"), 255));
        guia.setItemsResumen(trimToMax(buildItemsResumen(items), 2000));
        guia.setFacturacionEstado(GuiaRemision.ESTADO_PENDIENTE);
        guia.setFacturacionIntentos(0);
        guia.setFacturadorEndpoint("/guias");
        guia.setFacturadorTipoComprobante("09");
        guia.setFacturadorMensaje("Guia registrada en Azurion. Pendiente de envio al facturador.");
        guia.setFacturacionActualizadoEn(OffsetDateTime.now());
        return guiaRemisionRepository.save(guia);
    }

    private void markGuiaProcessing(GuiaRemision guia) {
        guia.setFacturacionEstado(GuiaRemision.ESTADO_PROCESANDO);
        guia.setFacturacionIntentos(guia.getFacturacionIntentos() == null ? 1 : guia.getFacturacionIntentos() + 1);
        guia.setFacturadorMensaje("Enviando guia de remision al facturador");
        guia.setFacturacionActualizadoEn(OffsetDateTime.now());
        guiaRemisionRepository.save(guia);
    }

    private void updateGuiaFromEmission(GuiaRemision guia, FacturadorClient.FacturadorEmissionResult emission) {
        JsonNode responseBody = emission.responseBody();
        guia.setFacturadorHttpStatus(emission.status());
        guia.setFacturadorEndpoint(emission.endpoint());
        guia.setFacturadorTipoComprobante("09");
        guia.setFacturadorMensaje(trimToMax(emission.message(), 500));
        guia.setFacturadorSunatEstado(normalizeEstado(readText(responseBody, "estado", "sunat_estado", "estado_sunat", "status")));
        guia.setFacturadorDocumentoId(readText(responseBody, "documento_id", "id_documento", "documentId"));
        guia.setFacturadorTicket(readText(responseBody, "ticket", "ticket_sunat"));
        guia.setFacturadorPdfUrl(readUrl(responseBody, "pdf_url", "url_pdf", "pdf"));
        guia.setFacturadorXmlUrl(readUrl(responseBody, "xml_url", "url_xml", "xml"));
        guia.setFacturadorCdrUrl(readUrl(responseBody, "cdr_url", "url_cdr", "cdr"));
        guia.setFacturadorRespuestaJson(responseBody == null ? null : responseBody.toString());
        guia.setFacturacionEstado(resolveFacturacionEstado(guia.getFacturadorSunatEstado(), emission.success()));
        guia.setFacturacionActualizadoEn(OffsetDateTime.now());
        guiaRemisionRepository.save(guia);
    }

    private void markGuiaError(GuiaRemision guia, String message) {
        guia.setFacturacionEstado(GuiaRemision.ESTADO_ERROR);
        guia.setFacturadorMensaje(trimToMax(message, 500));
        guia.setFacturacionActualizadoEn(OffsetDateTime.now());
        guiaRemisionRepository.save(guia);
    }

    private List<ResolvedGuiaItem> resolveItems(List<RegistrarGuiaRemisionRequest.GuiaItemRequest> items) {
        List<ResolvedGuiaItem> resolved = new ArrayList<>();
        for (RegistrarGuiaRemisionRequest.GuiaItemRequest item : items) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new BusinessException(
                            "GUIA_PRODUCTO_NO_ENCONTRADO",
                            "Producto no encontrado: " + item.productoId()
                    ));

            if (!producto.isActivo()) {
                throw new BusinessException("GUIA_PRODUCTO_INACTIVO", "No se puede enviar guia con productos inactivos.");
            }

            String descripcion = trim(item.descripcion());
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = producto.getNombre();
            }

            resolved.add(new ResolvedGuiaItem(producto, descripcion, item.cantidad()));
        }
        return resolved;
    }

    private Map<String, Object> buildPayload(
            RegistrarGuiaRemisionRequest request,
            Empresa empresa,
            Sucursal origen,
            Sucursal destino,
            List<ResolvedGuiaItem> items,
            String externalId,
            LocalDate fechaEmision,
            LocalDate fechaTraslado
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        Map<String, Object> empresaPayload = new LinkedHashMap<>();
        empresaPayload.put("ruc", empresa.getRuc());
        empresaPayload.put("razon_social", empresa.getRazonSocial());
        empresaPayload.put("direccion", buildSucursalAddress(origen));
        payload.put("empresa", empresaPayload);
        payload.put("cliente", Map.of(
                "tipo_doc", "6",
                "num_doc", empresa.getRuc(),
                "razon_social", empresa.getRazonSocial()
        ));

        List<Map<String, Object>> detalles = new ArrayList<>();
        for (ResolvedGuiaItem item : items) {
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("codigo", item.producto().getSku());
            detalle.put("descripcion", item.descripcion());
            detalle.put("unidad", "NIU");
            detalle.put("cantidad", item.cantidad());
            detalles.add(detalle);
        }
        payload.put("detalles", detalles);

        String motivoDescripcion = defaultIfBlank(trim(request.motivoTraslado()), "VENTA");
        String observacion = defaultIfBlank(trim(request.observacion()), "GUIA DE REMISION");
        String transportista = trim(request.transportista());
        if (transportista != null && !transportista.isBlank()) {
            observacion = observacion + " | Transportista: " + transportista;
        }

        Map<String, Object> documento = new LinkedHashMap<>();
        documento.put("fecha_emision", fechaEmision.toString());
        documento.put("fecha_traslado", fechaTraslado.toString());
        documento.put("external_id", externalId);
        documento.put("observacion", observacion);
        payload.put("documento", documento);

        Map<String, Object> traslado = new LinkedHashMap<>();
        traslado.put("modalidad", "01");
        traslado.put("motivo_codigo", "01");
        traslado.put("motivo_descripcion", motivoDescripcion);
        traslado.put("fecha_inicio", fechaTraslado.toString());
        traslado.put("peso_total", 1);
        traslado.put("unidad_peso", "KGM");
        traslado.put("numero_bultos", 1);
        traslado.put("partida", buildSucursalAddress(origen));
        traslado.put("llegada", buildSucursalAddress(destino));
        payload.put("traslado", traslado);

        payload.put("sucursal", Map.of(
                "codigo", origen.getCodigo(),
                "ubigeo", origen.getUbigeoCodigo()
        ));
        return payload;
    }

    private Map<String, Object> buildSucursalAddress(Sucursal sucursal) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("ubigeo", sucursal.getUbigeoCodigo());
        address.put("direccion", defaultIfBlank(trim(sucursal.getDireccion()), sucursal.getNombre()));
        address.put("departamento", sucursal.getDepartamento());
        address.put("provincia", sucursal.getProvincia());
        address.put("distrito", sucursal.getDistrito());
        address.put("cod_local", "0000");
        return address;
    }

    private LocalDate parseFechaTraslado(String raw) {
        String input = trim(raw);
        if (input == null || input.isBlank()) {
            throw new BusinessException("GUIA_FECHA_TRASLADO_INVALIDA", "La fecha de traslado es obligatoria.");
        }

        String datePart = input.length() >= 10 ? input.substring(0, 10) : input;
        try {
            return LocalDate.parse(datePart);
        } catch (DateTimeParseException exception) {
            throw new BusinessException("GUIA_FECHA_TRASLADO_INVALIDA", "La fecha de traslado no tiene formato valido.");
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private String buildItemsResumen(List<ResolvedGuiaItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (ResolvedGuiaItem item : items) {
            parts.add(item.producto().getSku() + " - " + item.descripcion() + " x " + item.cantidad());
        }
        return String.join("; ", parts);
    }

    private String resolveFacturacionEstado(String sunatEstado, boolean success) {
        String estado = normalizeEstado(sunatEstado);
        if (GuiaRemision.ESTADO_ACEPTADO.equals(estado)
                || GuiaRemision.ESTADO_RECHAZADO.equals(estado)
                || GuiaRemision.ESTADO_ERROR.equals(estado)) {
            return estado;
        }
        return success ? GuiaRemision.ESTADO_PROCESANDO : GuiaRemision.ESTADO_ERROR;
    }

    private String normalizeEstado(String raw) {
        String value = trim(raw);
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "ACEPTADO", "ACCEPTED", "PROCESADO" -> GuiaRemision.ESTADO_ACEPTADO;
            case "RECHAZADO", "REJECTED" -> GuiaRemision.ESTADO_RECHAZADO;
            case "ERROR", "FAILED" -> GuiaRemision.ESTADO_ERROR;
            case "EN_PROCESO", "PROCESANDO", "PROCESSING" -> GuiaRemision.ESTADO_PROCESANDO;
            case "RECIBIDO", "REGISTERED", "REGISTRADO", "PENDIENTE", "PENDING", "EN_COLA", "QUEUED", "RECEIVED"
                    -> GuiaRemision.ESTADO_PENDIENTE;
            default -> value.toUpperCase(Locale.ROOT);
        };
    }

    private String readUrl(JsonNode source, String... keys) {
        String value = readText(source, keys);
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "http://", 0, 7)
                || trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            return trimToMax(trimmed, 500);
        }
        return null;
    }

    private String readText(JsonNode source, String... keys) {
        JsonNode node = deepFindNode(source, keys);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return trimToMax(node.asText(null), 500);
    }

    private JsonNode deepFindNode(JsonNode source, String... keys) {
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }

        for (String key : keys) {
            if (source.has(key)) {
                JsonNode value = source.get(key);
                if (value != null && !value.isNull() && !value.isMissingNode() && !value.asText("").isBlank()) {
                    return value;
                }
            }
        }

        if (source.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = source.fields();
            while (iterator.hasNext()) {
                JsonNode nested = deepFindNode(iterator.next().getValue(), keys);
                if (nested != null && !nested.isNull() && !nested.isMissingNode() && !nested.asText("").isBlank()) {
                    return nested;
                }
            }
        }

        if (source.isArray()) {
            for (JsonNode item : source) {
                JsonNode nested = deepFindNode(item, keys);
                if (nested != null && !nested.isNull() && !nested.isMissingNode() && !nested.asText("").isBlank()) {
                    return nested;
                }
            }
        }
        return null;
    }

    private GuiaRemisionResponse toResponse(GuiaRemision guia) {
        return new GuiaRemisionResponse(
                guia.getId(),
                guia.getExternalId(),
                guia.getSucursalOrigenId(),
                guia.getSucursalOrigenNombre(),
                guia.getSucursalDestinoId(),
                guia.getSucursalDestinoNombre(),
                guia.getFechaEmision(),
                guia.getFechaTraslado(),
                guia.getMotivoTraslado(),
                guia.getTransportista(),
                guia.getObservacion(),
                guia.getResponsableId(),
                guia.getResponsableNombre(),
                guia.getItemsResumen(),
                guia.getFacturacionEstado(),
                guia.getFacturacionIntentos(),
                guia.getFacturadorHttpStatus(),
                guia.getFacturadorEndpoint(),
                guia.getFacturadorTipoComprobante(),
                guia.getFacturadorMensaje(),
                guia.getFacturadorSunatEstado(),
                guia.getFacturadorDocumentoId(),
                guia.getFacturadorTicket(),
                guia.getFacturadorPdfUrl(),
                guia.getFacturadorXmlUrl(),
                guia.getFacturadorCdrUrl(),
                guia.getFacturadorRespuestaJson(),
                guia.getFacturacionActualizadoEn()
        );
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private record ResolvedGuiaItem(
            Producto producto,
            String descripcion,
            java.math.BigDecimal cantidad
    ) {
    }
}
