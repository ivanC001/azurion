package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.caja.application.dto.FacturadorVentaResponse;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.application.dto.RegistrarNotaFiscalRequest;
import com.azurion.saascore.facturacion.application.dto.RegistrarNotaFiscalResponse;
import com.azurion.saascore.facturacion.application.mappers.NotaFiscalMapper;
import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import com.azurion.saascore.facturacion.domain.repositories.NotaFiscalRepository;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.entities.VentaDetalle;
import com.azurion.saascore.ventas.domain.repositories.VentaDetalleRepository;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarNotaFiscalUseCase {

    private final EmpresaRepository empresaRepository;
    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final NotaFiscalRepository notaFiscalRepository;
    private final FacturadorClient facturadorClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public RegistrarNotaFiscalResponse execute(String tipoDocumento, RegistrarNotaFiscalRequest request) {
        String normalizedTipoDocumento = normalizeNotaTipoDocumento(tipoDocumento);
        String tipoNota = NotaFiscal.TIPO_DOCUMENTO_CREDITO.equals(normalizedTipoDocumento)
                ? NotaFiscal.TIPO_NOTA_CREDITO
                : NotaFiscal.TIPO_NOTA_DEBITO;

        String tenantId = TenantContext.getTenantId();
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("TENANT_NO_ENCONTRADO", "Empresa no encontrada para tenant: " + tenantId));

        Venta venta = ventaRepository.findById(request.ventaId())
                .orElseThrow(() -> new BusinessException("VENTA_NO_ENCONTRADA", "Venta referenciada no encontrada."));

        if (NotaFiscal.TIPO_DOCUMENTO_CREDITO.equals(normalizedTipoDocumento)
                && request.monto().compareTo(venta.getTotal()) > 0) {
            throw new BusinessException("NOTA_CREDITO_MONTO_INVALIDO", "La nota de credito no puede superar el total de la venta.");
        }

        DocumentoReferencia referencia = resolveDocumentoReferencia(tenantId, empresa.getRuc(), venta);
        if (!"01".equals(referencia.tipoDocumento()) && !"03".equals(referencia.tipoDocumento())) {
            throw new BusinessException(
                    "NOTA_REFERENCIA_INVALIDA",
                    "Solo se puede emitir nota sobre factura o boleta ya emitida por el facturador."
            );
        }

        LocalDate fechaEmision = LocalDate.now(ZoneId.of("America/Lima"));
        String externalId = buildExternalId(normalizedTipoDocumento);
        String endpoint = NotaFiscal.TIPO_DOCUMENTO_CREDITO.equals(normalizedTipoDocumento)
                ? "/notas-credito"
                : "/notas-debito";

        NotaFiscal nota = createNota(
                request,
                venta,
                referencia,
                normalizedTipoDocumento,
                tipoNota,
                externalId,
                endpoint,
                fechaEmision
        );

        Map<String, Object> payload = buildPayload(
                request,
                empresa,
                venta,
                nota,
                referencia,
                fechaEmision
        );

        markNotaProcessing(nota);

        try {
            FacturadorClient.FacturadorEmissionResult emission = facturadorClient.emitirDocumento(
                    tenantId,
                    empresa.getRuc(),
                    endpoint,
                    payload,
                    normalizedTipoDocumento
            );

            updateNotaFromEmission(nota, emission);

            return new RegistrarNotaFiscalResponse(
                    externalId,
                    NotaFiscalMapper.toResponse(nota),
                    new FacturadorVentaResponse(
                            emission.success(),
                            emission.status(),
                            emission.endpoint(),
                            normalizedTipoDocumento,
                            emission.message(),
                            emission.responseBody()
                    )
            );
        } catch (BusinessException exception) {
            markNotaError(nota, exception.getMessage());
            return new RegistrarNotaFiscalResponse(
                    externalId,
                    NotaFiscalMapper.toResponse(nota),
                    new FacturadorVentaResponse(
                            false,
                            500,
                            endpoint,
                            normalizedTipoDocumento,
                            exception.getMessage(),
                            null
                    )
            );
        } catch (Exception exception) {
            String message = "No se pudo enviar nota al facturador: " + exception.getMessage();
            markNotaError(nota, message);
            return new RegistrarNotaFiscalResponse(
                    externalId,
                    NotaFiscalMapper.toResponse(nota),
                    new FacturadorVentaResponse(
                            false,
                            500,
                            endpoint,
                            normalizedTipoDocumento,
                            message,
                            null
                    )
            );
        }
    }

    private NotaFiscal createNota(
            RegistrarNotaFiscalRequest request,
            Venta venta,
            DocumentoReferencia referencia,
            String tipoDocumento,
            String tipoNota,
            String externalId,
            String endpoint,
            LocalDate fechaEmision
    ) {
        NotaFiscal nota = new NotaFiscal();
        nota.setExternalId(externalId);
        nota.setTipoDocumento(tipoDocumento);
        nota.setTipoNota(tipoNota);
        nota.setVentaId(venta.getId());
        nota.setVentaExternalId(venta.getExternalId());
        nota.setVentaTipoDocumento(referencia.tipoDocumento());
        nota.setVentaNumeroDocumento(referencia.numeroDocumento());
        nota.setClienteDocumento(defaultIfBlank(venta.getClienteDocumento(), "-"));
        nota.setClienteNombre(defaultIfBlank(venta.getClienteNombre(), "CLIENTE"));
        nota.setMoneda(resolveMoneda(venta.getMoneda()));
        nota.setMonto(scaleMoney(request.monto()));
        nota.setFechaEmision(fechaEmision);
        nota.setMotivoCodigo(trimToMax(request.motivoCodigo(), 6));
        nota.setMotivoDescripcion(trimToMax(request.motivoDescripcion(), 255));
        nota.setResponsableId(trimToMax(defaultIfBlank(request.responsableId(), "system"), 120));
        nota.setResponsableNombre(trimToMax(defaultIfBlank(request.responsableNombre(), "Usuario"), 255));
        nota.setFacturacionEstado(NotaFiscal.ESTADO_PENDIENTE);
        nota.setFacturacionIntentos(0);
        nota.setFacturadorEndpoint(endpoint);
        nota.setFacturadorTipoComprobante(tipoDocumento);
        nota.setFacturadorMensaje("Nota registrada en Azurion. Pendiente de envio al facturador.");
        nota.setFacturacionActualizadoEn(OffsetDateTime.now());
        return notaFiscalRepository.save(nota);
    }

    private void markNotaProcessing(NotaFiscal nota) {
        nota.setFacturacionEstado(NotaFiscal.ESTADO_PROCESANDO);
        nota.setFacturacionIntentos(nota.getFacturacionIntentos() == null ? 1 : nota.getFacturacionIntentos() + 1);
        nota.setFacturadorMensaje("Enviando nota al facturador");
        nota.setFacturacionActualizadoEn(OffsetDateTime.now());
        notaFiscalRepository.save(nota);
    }

    private void updateNotaFromEmission(NotaFiscal nota, FacturadorClient.FacturadorEmissionResult emission) {
        JsonNode responseBody = emission.responseBody();
        nota.setFacturadorHttpStatus(emission.status());
        nota.setFacturadorEndpoint(emission.endpoint());
        nota.setFacturadorTipoComprobante(nota.getTipoDocumento());
        nota.setFacturadorMensaje(trimToMax(emission.message(), 500));
        nota.setFacturadorSunatEstado(normalizeEstado(readText(responseBody, "estado", "sunat_estado", "estado_sunat", "status")));
        nota.setFacturadorDocumentoId(readText(responseBody, "documento_id", "id_documento", "documentId"));
        nota.setFacturadorTicket(readText(responseBody, "ticket", "ticket_sunat"));
        nota.setFacturadorPdfUrl(readUrl(responseBody, "pdf_url", "url_pdf", "pdf"));
        nota.setFacturadorXmlUrl(readUrl(responseBody, "xml_url", "url_xml", "xml"));
        nota.setFacturadorCdrUrl(readUrl(responseBody, "cdr_url", "url_cdr", "cdr"));
        nota.setFacturadorRespuestaJson(responseBody == null ? null : responseBody.toString());
        nota.setFacturacionEstado(resolveFacturacionEstado(nota.getFacturadorSunatEstado(), emission.success()));
        nota.setFacturacionActualizadoEn(OffsetDateTime.now());
        notaFiscalRepository.save(nota);
    }

    private void markNotaError(NotaFiscal nota, String message) {
        nota.setFacturacionEstado(NotaFiscal.ESTADO_ERROR);
        nota.setFacturadorMensaje(trimToMax(message, 500));
        nota.setFacturacionActualizadoEn(OffsetDateTime.now());
        notaFiscalRepository.save(nota);
    }

    private Map<String, Object> buildPayload(
            RegistrarNotaFiscalRequest request,
            Empresa empresa,
            Venta venta,
            NotaFiscal nota,
            DocumentoReferencia referencia,
            LocalDate fechaEmision
    ) {
        BigDecimal total = scaleMoney(request.monto());
        List<VentaDetalle> ventaDetalles = ventaDetalleRepository.findByVentaIdOrderByIdAsc(venta.getId());
        if (ventaDetalles.isEmpty()) {
            throw new BusinessException("VENTA_SIN_DETALLE_TRIBUTARIO", "La venta no tiene detalle tributario congelado para emitir la nota");
        }
        BigDecimal originalBase = ventaDetalles.stream()
                .map(VentaDetalle::getBaseImponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ratio = total.divide(venta.getTotal(), 8, RoundingMode.HALF_UP);
        BigDecimal base = scaleMoney(originalBase.multiply(ratio));
        BigDecimal igv = scaleMoney(total.subtract(base));
        BigDecimal porcentajeIgv = base.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : igv.multiply(new BigDecimal("100")).divide(base, 2, RoundingMode.HALF_UP);
        VentaDetalle referenciaTributaria = ventaDetalles.getFirst();

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("codigo", nota.getTipoDocumento().equals(NotaFiscal.TIPO_DOCUMENTO_CREDITO) ? "NC-AJUSTE" : "ND-AJUSTE");
        detalle.put("descripcion", nota.getMotivoDescripcion());
        detalle.put("unidad", "NIU");
        detalle.put("cantidad", BigDecimal.ONE);
        detalle.put("precio_unitario", total);
        detalle.put("valor_unitario", base);
        detalle.put("mto_valor_unitario", base);
        detalle.put("mto_precio_unitario", total);
        detalle.put("mto_valor_venta", base);
        detalle.put("igv", igv);
        detalle.put("porcentaje_igv", porcentajeIgv);
        detalle.put("tip_afe_igv", referenciaTributaria.getTipoAfectacionIgvCodigo());
        detalle.put("tributo_codigo", referenciaTributaria.getTributoCodigo());
        detalle.put("total_impuestos", igv);
        detalle.put("total", total);

        Map<String, Object> documento = new LinkedHashMap<>();
        documento.put("tipo", nota.getTipoDocumento());
        documento.put("fecha_emision", fechaEmision.toString());
        documento.put("moneda", resolveMoneda(venta.getMoneda()));
        documento.put("total", total);
        documento.put("external_id", nota.getExternalId());
        documento.put("codigo_motivo", nota.getMotivoCodigo());
        documento.put("descripcion_motivo", nota.getMotivoDescripcion());
        Map<String, Object> referenciaPayload = new LinkedHashMap<>();
        referenciaPayload.put("tipo_doc", referencia.tipoDocumento());
        referenciaPayload.put("nro_doc", referencia.numeroDocumento());
        documento.put("referencia", referenciaPayload);
        documento.put("tip_doc_afectado", referencia.tipoDocumento());
        documento.put("num_doc_afectado", referencia.numeroDocumento());
        documento.put("tipo_operacion", referenciaTributaria.getTipoOperacionCodigo());
        putOperationAmount(documento, referenciaTributaria.getTipoAfectacionIgvCodigo(), base);
        documento.put("igv_total", igv);
        documento.put("total_impuestos", igv);
        documento.put("valor_venta", base);
        documento.put("sub_total", total);
        documento.put("mto_imp_venta", total);
        documento.put("observacion", nota.getMotivoDescripcion());

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> empresaPayload = new LinkedHashMap<>();
        empresaPayload.put("ruc", defaultIfBlank(empresa.getRuc(), "-"));
        empresaPayload.put("razon_social", defaultIfBlank(empresa.getRazonSocial(), "EMPRESA"));
        payload.put("empresa", empresaPayload);
        payload.put("cliente", buildCliente(venta));
        payload.put("documento", documento);
        payload.put("detalles", List.of(detalle));
        return payload;
    }

    private void putOperationAmount(Map<String, Object> documento, String afectacion, BigDecimal base) {
        documento.put("mto_oper_gravadas", BigDecimal.ZERO);
        documento.put("mto_oper_exoneradas", BigDecimal.ZERO);
        documento.put("mto_oper_inafectas", BigDecimal.ZERO);
        documento.put("mto_oper_exportacion", BigDecimal.ZERO);
        if ("20".equals(afectacion) || "21".equals(afectacion)) {
            documento.put("mto_oper_exoneradas", base);
        } else if ("40".equals(afectacion)) {
            documento.put("mto_oper_exportacion", base);
        } else if (afectacion != null && afectacion.startsWith("3")) {
            documento.put("mto_oper_inafectas", base);
        } else {
            documento.put("mto_oper_gravadas", base);
        }
    }

    private Map<String, Object> buildCliente(Venta venta) {
        String documento = defaultIfBlank(venta.getClienteDocumento(), "-");
        String tipoDoc = inferTipoDocumento(documento);
        return Map.of(
                "tipo_doc", tipoDoc,
                "num_doc", documento,
                "razon_social", defaultIfBlank(venta.getClienteNombre(), "CLIENTE")
        );
    }

    private DocumentoReferencia resolveDocumentoReferencia(String tenantId, String tenantRuc, Venta venta) {
        FacturadorClient.FacturadorDocumentoStatusResult remote = null;
        try {
            remote = facturadorClient
                    .consultarDocumentosPorExternalIds(tenantId, tenantRuc, List.of(venta.getExternalId()))
                    .get(venta.getExternalId());
        } catch (Exception ignored) {
            remote = null;
        }

        String tipoDocumento = normalizeVentaTipoDocumento(firstNonBlank(
                remote == null ? null : remote.tipoDocumento(),
                venta.getFacturadorTipoComprobante(),
                readText(parseJson(venta.getFacturadorRespuestaJson()), "tipoDocumento", "tipo_documento")
        ));

        String serie = firstNonBlank(
                remote == null ? null : remote.serie(),
                readText(parseJson(venta.getFacturadorRespuestaJson()), "serie")
        );
        String correlativo = firstNonBlank(
                remote == null ? null : remote.correlativo(),
                readText(parseJson(venta.getFacturadorRespuestaJson()), "correlativo")
        );

        if (tipoDocumento == null && serie != null) {
            tipoDocumento = serie.toUpperCase(Locale.ROOT).startsWith("B") ? "03" : "01";
        }

        if (serie == null || correlativo == null || tipoDocumento == null) {
            throw new BusinessException(
                    "VENTA_REFERENCIA_NO_DISPONIBLE",
                    "La venta aun no tiene serie/correlativo del facturador. Recarga ventas o espera el callback antes de emitir la nota."
            );
        }

        return new DocumentoReferencia(tipoDocumento, serie + "-" + correlativo);
    }

    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeNotaTipoDocumento(String tipoDocumento) {
        String normalized = firstNonBlank(tipoDocumento);
        if (normalized == null) {
            throw new BusinessException("NOTA_TIPO_INVALIDO", "Tipo de nota invalido.");
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "07", "CREDITO", "NOTA_CREDITO" -> NotaFiscal.TIPO_DOCUMENTO_CREDITO;
            case "08", "DEBITO", "NOTA_DEBITO" -> NotaFiscal.TIPO_DOCUMENTO_DEBITO;
            default -> throw new BusinessException("NOTA_TIPO_INVALIDO", "Tipo de nota invalido: " + tipoDocumento);
        };
    }

    private String normalizeVentaTipoDocumento(String raw) {
        String normalized = firstNonBlank(raw);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "FACTURA", "01" -> "01";
            case "BOLETA", "03" -> "03";
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }

    private String resolveFacturacionEstado(String sunatEstado, boolean success) {
        String estado = normalizeEstado(sunatEstado);
        if (NotaFiscal.ESTADO_ACEPTADO.equals(estado)
                || NotaFiscal.ESTADO_RECHAZADO.equals(estado)
                || NotaFiscal.ESTADO_ERROR.equals(estado)) {
            return estado;
        }
        return success ? NotaFiscal.ESTADO_PROCESANDO : NotaFiscal.ESTADO_ERROR;
    }

    private String normalizeEstado(String raw) {
        String value = firstNonBlank(raw);
        if (value == null) {
            return null;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "ACEPTADO", "ACCEPTED", "PROCESADO" -> NotaFiscal.ESTADO_ACEPTADO;
            case "RECHAZADO", "REJECTED" -> NotaFiscal.ESTADO_RECHAZADO;
            case "ERROR", "FAILED" -> NotaFiscal.ESTADO_ERROR;
            case "EN_PROCESO", "PROCESANDO", "PROCESSING" -> NotaFiscal.ESTADO_PROCESANDO;
            case "RECIBIDO", "REGISTERED", "REGISTRADO", "PENDIENTE", "PENDING", "EN_COLA", "QUEUED", "RECEIVED"
                    -> NotaFiscal.ESTADO_PENDIENTE;
            default -> value.toUpperCase(Locale.ROOT);
        };
    }

    private String inferTipoDocumento(String numeroDocumento) {
        String normalized = defaultIfBlank(numeroDocumento, "-");
        if (normalized.matches("^[0-9]{11}$")) {
            return "6";
        }
        if (normalized.matches("^[0-9]{8}$")) {
            return "1";
        }
        return "0";
    }

    private String resolveMoneda(String moneda) {
        String normalized = defaultIfBlank(moneda, "PEN");
        return trimToMax(normalized.toUpperCase(Locale.ROOT), 3);
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
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }
        for (String key : keys) {
            JsonNode direct = source.path(key);
            if (!direct.isMissingNode() && !direct.isNull()) {
                String value = firstNonBlank(direct.asText(null));
                if (value != null) {
                    return trimToMax(value, 500);
                }
            }
        }
        if (source.isObject()) {
            for (JsonNode nested : source) {
                String value = readText(nested, keys);
                if (value != null) {
                    return value;
                }
            }
        }
        if (source.isArray()) {
            for (JsonNode nested : source) {
                String value = readText(nested, keys);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildExternalId(String tipoDocumento) {
        String prefix = NotaFiscal.TIPO_DOCUMENTO_CREDITO.equals(tipoDocumento) ? "NC" : "ND";
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.trim().isBlank()) {
            return defaultValue;
        }
        return value.trim();
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record DocumentoReferencia(String tipoDocumento, String numeroDocumento) {
    }
}
