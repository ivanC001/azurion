package com.azurion.saascore.facturacion.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.domain.entities.GuiaRemision;
import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import com.azurion.saascore.facturacion.domain.repositories.GuiaRemisionRepository;
import com.azurion.saascore.facturacion.domain.repositories.NotaFiscalRepository;
import com.azurion.saascore.ventas.application.dto.VentaStatusRealtimeEvent;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.saascore.ventas.infrastructure.realtime.VentaStatusRealtimeStreamService;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ProcessFacturadorVentaCallbackUseCase {

    private final EmpresaRepository empresaRepository;
    private final VentaRepository ventaRepository;
    private final GuiaRemisionRepository guiaRemisionRepository;
    private final NotaFiscalRepository notaFiscalRepository;
    private final PlatformTransactionManager transactionManager;
    private final VentaStatusRealtimeStreamService ventaStatusRealtimeStreamService;

    public CallbackProcessResult execute(JsonNode payload) {
        return execute(payload, "DOCUMENTOS");
    }

    public CallbackProcessResult execute(JsonNode payload, String callbackChannel) {
        String normalizedChannel = normalizeCallbackChannel(callbackChannel);
        String tipoDocumento = normalizeTipoDocumento(firstNonBlank(
                readText(payload, "tipoDocumento", "tipo_documento"),
                readText(payload.path("documento"), "tipo", "tipo_documento", "tipoDocumento"),
                resolveTipoDocumentoByCallbackChannel(normalizedChannel)
        ));

        String externalId = firstNonBlank(
                readText(payload, "externalId", "external_id"),
                readText(payload.path("documento"), "external_id", "externalId")
        );
        if (externalId == null) {
            throw new BusinessException("FACTURADOR_CALLBACK_EXTERNAL_ID_REQUIRED", "El callback no contiene externalId del documento.");
        }

        String ruc = firstNonBlank(
                readText(payload, "tenantRuc", "tenant_ruc"),
                readText(payload, "ruc", "empresaRuc", "empresa_ruc"),
                readText(payload.path("empresa"), "ruc")
        );
        if (ruc == null) {
            throw new BusinessException("FACTURADOR_CALLBACK_RUC_REQUIRED", "El callback no contiene RUC de empresa.");
        }

        Empresa empresa = empresaRepository.findByRuc(ruc)
                .orElseThrow(() -> new BusinessException("FACTURADOR_CALLBACK_EMPRESA_NOT_FOUND", "No existe empresa asociada al RUC " + ruc + "."));

        String previousTenant = TenantContext.getTenantId();
        TenantContext.setTenantId(empresa.getTenantId());
        try {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            AtomicReference<CallbackProcessResult> output = new AtomicReference<>();

            tx.executeWithoutResult(status -> {
                Venta venta = ventaRepository.findByExternalId(externalId).orElse(null);
                if (venta == null) {
                    NotaFiscal nota = notaFiscalRepository.findByExternalId(externalId).orElse(null);
                    if (nota != null) {
                        updateNotaFromCallback(nota, payload, tipoDocumento);
                        output.set(new CallbackProcessResult(
                                null,
                                empresa.getTenantId(),
                                nota.getExternalId(),
                                nota.getFacturacionEstado(),
                                normalizedChannel,
                                firstNonBlank(tipoDocumento, nota.getFacturadorTipoComprobante()),
                                null
                        ));
                        return;
                    }

                    GuiaRemision guia = guiaRemisionRepository.findByExternalId(externalId).orElse(null);
                    if (guia != null) {
                        updateGuiaFromCallback(guia, payload, tipoDocumento);
                        output.set(new CallbackProcessResult(
                                null,
                                empresa.getTenantId(),
                                guia.getExternalId(),
                                guia.getFacturacionEstado(),
                                normalizedChannel,
                                firstNonBlank(tipoDocumento, guia.getFacturadorTipoComprobante()),
                                null
                        ));
                        return;
                    }

                    if (shouldIgnoreMissingVenta(externalId, tipoDocumento)) {
                        output.set(new CallbackProcessResult(
                                null,
                                empresa.getTenantId(),
                                externalId,
                                "IGNORED",
                                normalizedChannel,
                                tipoDocumento,
                                null
                        ));
                        return;
                    }
                    throw new BusinessException("FACTURADOR_CALLBACK_VENTA_NOT_FOUND", "No existe venta con externalId " + externalId + ".");
                }

                String tipoDocumentoResolved = normalizeTipoDocumento(firstNonBlank(
                        tipoDocumento,
                        venta.getFacturadorTipoComprobante()
                ));

                String estadoInterno = normalizeEstado(readText(payload, "estado", "estadoInterno", "estado_interno", "facturadorEstado"));
                String estadoSunat = normalizeEstado(firstNonBlank(
                        readText(payload, "sunatEstado", "sunat_estado"),
                        readText(payload.path("sunat"), "estado")
                ));
                String codigoSunat = trimToMax(firstNonBlank(
                        readText(payload, "sunatCodigo", "sunat_codigo", "sunat_codigo_error"),
                        readText(payload.path("sunat"), "codigo", "codigo_error")
                ), 120);
                String mensajeSunat = trimToMax(firstNonBlank(
                        readText(payload, "sunatMensaje", "sunat_mensaje"),
                        readText(payload.path("sunat"), "mensaje")
                ), 500);
                String mensaje = trimToMax(resolveMessage(payload, codigoSunat, mensajeSunat, estadoInterno), 500);

                venta.setFacturacionEstado(resolveFacturacionEstado(tipoDocumentoResolved, estadoInterno, estadoSunat));
                venta.setFacturadorSunatEstado(trimToMax(firstNonBlank(estadoSunat, estadoInterno), 30));
                venta.setFacturadorDocumentoId(trimToMax(firstNonBlank(
                        readText(payload, "documentoId", "documento_id"),
                        readText(payload.path("documento"), "id", "documento_id")
                ), 80));
                venta.setFacturadorTicket(trimToMax(firstNonBlank(
                        readText(payload, "ticket"),
                        readText(payload.path("sunat"), "ticket")
                ), 120));
                venta.setFacturadorPdfUrl(normalizeUrl(readText(payload, "pdfUrl", "pdf_url")));
                venta.setFacturadorXmlUrl(normalizeUrl(readText(payload, "xmlUrl", "xml_url")));
                venta.setFacturadorCdrUrl(normalizeUrl(readText(payload, "cdrUrl", "cdr_url")));
                venta.setFacturadorRespuestaJson(payload == null ? null : payload.toString());
                venta.setFacturadorMensaje(mensaje);
                venta.setFacturadorEndpoint(resolveEndpointByTipoDocumento(tipoDocumentoResolved));
                if (tipoDocumentoResolved != null) {
                    venta.setFacturadorTipoComprobante(trimToMax(tipoDocumentoResolved, 30));
                }

                Integer callbackHttpStatus = readInt(payload, "httpStatus", "statusCode", "status");
                if (callbackHttpStatus != null && callbackHttpStatus > 0) {
                    venta.setFacturadorHttpStatus(callbackHttpStatus);
                }

                venta.setFacturacionActualizadoEn(OffsetDateTime.now());
                ventaRepository.save(venta);

                output.set(new CallbackProcessResult(
                        venta.getId(),
                        empresa.getTenantId(),
                        venta.getExternalId(),
                        venta.getFacturacionEstado(),
                        normalizedChannel,
                        tipoDocumentoResolved,
                        VentaStatusRealtimeEvent.fromVenta(empresa.getTenantId(), "FACTURADOR_CALLBACK", venta)
                ));
            });

            CallbackProcessResult result = output.get();
            if (result != null && result.realtimeEvent() != null) {
                ventaStatusRealtimeStreamService.publish(result.realtimeEvent());
            }
            return result;
        } finally {
            if (previousTenant == null || previousTenant.isBlank()) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
    }

    private String resolveMessage(JsonNode payload, String codigoSunat, String mensajeSunat, String estadoInterno) {
        if (codigoSunat != null && mensajeSunat != null) {
            return codigoSunat + " - " + mensajeSunat;
        }
        String plain = firstNonBlank(
                mensajeSunat,
                readText(payload, "message", "mensaje", "detail", "detalle"),
                readText(payload.path("sunat"), "message", "detail", "detalle")
        );
        if (plain != null) {
            return plain;
        }
        if (codigoSunat != null) {
            return "Codigo SUNAT " + codigoSunat;
        }
        if (estadoInterno != null) {
            return "Estado actualizado: " + estadoInterno;
        }
        return "Estado actualizado desde facturador";
    }

    private String resolveFacturacionEstado(String tipoDocumento, String estadoInterno, String estadoSunat) {
        if (isTerminal(estadoSunat)) {
            return estadoSunat;
        }
        if (isTerminal(estadoInterno)) {
            return estadoInterno;
        }

        if ("TK".equalsIgnoreCase(tipoDocumento)) {
            return Venta.FACTURACION_ESTADO_ACEPTADO;
        }

        String moving = firstNonBlank(estadoSunat, estadoInterno);
        if (Venta.FACTURACION_ESTADO_PROCESANDO.equals(moving)) {
            return Venta.FACTURACION_ESTADO_PROCESANDO;
        }
        if (Venta.FACTURACION_ESTADO_PENDIENTE.equals(moving)) {
            return Venta.FACTURACION_ESTADO_PENDIENTE;
        }
        if (moving != null) {
            return Venta.FACTURACION_ESTADO_PROCESANDO;
        }

        return Venta.FACTURACION_ESTADO_PENDIENTE;
    }

    private boolean isTerminal(String estado) {
        return Venta.FACTURACION_ESTADO_ACEPTADO.equals(estado)
                || Venta.FACTURACION_ESTADO_RECHAZADO.equals(estado)
                || Venta.FACTURACION_ESTADO_ERROR.equals(estado);
    }

    private void updateNotaFromCallback(NotaFiscal nota, JsonNode payload, String tipoDocumento) {
        String estadoInterno = normalizeEstado(readText(payload, "estado", "estadoInterno", "estado_interno", "facturadorEstado"));
        String estadoSunat = normalizeEstado(firstNonBlank(
                readText(payload, "sunatEstado", "sunat_estado"),
                readText(payload.path("sunat"), "estado")
        ));
        String codigoSunat = trimToMax(firstNonBlank(
                readText(payload, "sunatCodigo", "sunat_codigo", "sunat_codigo_error"),
                readText(payload.path("sunat"), "codigo", "codigo_error")
        ), 120);
        String mensajeSunat = trimToMax(firstNonBlank(
                readText(payload, "sunatMensaje", "sunat_mensaje"),
                readText(payload.path("sunat"), "mensaje")
        ), 500);
        String tipoDocumentoResolved = normalizeTipoDocumento(firstNonBlank(tipoDocumento, nota.getTipoDocumento()));

        nota.setFacturacionEstado(resolveFacturacionEstado(tipoDocumentoResolved, estadoInterno, estadoSunat));
        nota.setFacturadorSunatEstado(trimToMax(firstNonBlank(estadoSunat, estadoInterno), 30));
        nota.setFacturadorDocumentoId(trimToMax(firstNonBlank(
                readText(payload, "documentoId", "documento_id"),
                readText(payload.path("documento"), "id", "documento_id")
        ), 80));
        nota.setFacturadorTicket(trimToMax(firstNonBlank(
                readText(payload, "ticket"),
                readText(payload.path("sunat"), "ticket")
        ), 120));
        nota.setFacturadorPdfUrl(normalizeUrl(readText(payload, "pdfUrl", "pdf_url")));
        nota.setFacturadorXmlUrl(normalizeUrl(readText(payload, "xmlUrl", "xml_url")));
        nota.setFacturadorCdrUrl(normalizeUrl(readText(payload, "cdrUrl", "cdr_url")));
        nota.setFacturadorRespuestaJson(payload == null ? null : payload.toString());
        nota.setFacturadorMensaje(trimToMax(resolveMessage(payload, codigoSunat, mensajeSunat, estadoInterno), 500));
        nota.setFacturadorEndpoint(resolveEndpointByTipoDocumento(tipoDocumentoResolved));
        if (tipoDocumentoResolved != null) {
            nota.setFacturadorTipoComprobante(trimToMax(tipoDocumentoResolved, 30));
        }

        Integer callbackHttpStatus = readInt(payload, "httpStatus", "statusCode", "status");
        if (callbackHttpStatus != null && callbackHttpStatus > 0) {
            nota.setFacturadorHttpStatus(callbackHttpStatus);
        }

        nota.setFacturacionActualizadoEn(OffsetDateTime.now());
        notaFiscalRepository.save(nota);
    }

    private void updateGuiaFromCallback(GuiaRemision guia, JsonNode payload, String tipoDocumento) {
        String estadoInterno = normalizeEstado(readText(payload, "estado", "estadoInterno", "estado_interno", "facturadorEstado"));
        String estadoSunat = normalizeEstado(firstNonBlank(
                readText(payload, "sunatEstado", "sunat_estado"),
                readText(payload.path("sunat"), "estado")
        ));
        String codigoSunat = trimToMax(firstNonBlank(
                readText(payload, "sunatCodigo", "sunat_codigo", "sunat_codigo_error"),
                readText(payload.path("sunat"), "codigo", "codigo_error")
        ), 120);
        String mensajeSunat = trimToMax(firstNonBlank(
                readText(payload, "sunatMensaje", "sunat_mensaje"),
                readText(payload.path("sunat"), "mensaje")
        ), 500);

        guia.setFacturacionEstado(resolveFacturacionEstado("09", estadoInterno, estadoSunat));
        guia.setFacturadorSunatEstado(trimToMax(firstNonBlank(estadoSunat, estadoInterno), 30));
        guia.setFacturadorDocumentoId(trimToMax(firstNonBlank(
                readText(payload, "documentoId", "documento_id"),
                readText(payload.path("documento"), "id", "documento_id")
        ), 80));
        guia.setFacturadorTicket(trimToMax(firstNonBlank(
                readText(payload, "ticket"),
                readText(payload.path("sunat"), "ticket")
        ), 120));
        guia.setFacturadorPdfUrl(normalizeUrl(readText(payload, "pdfUrl", "pdf_url")));
        guia.setFacturadorXmlUrl(normalizeUrl(readText(payload, "xmlUrl", "xml_url")));
        guia.setFacturadorCdrUrl(normalizeUrl(readText(payload, "cdrUrl", "cdr_url")));
        guia.setFacturadorRespuestaJson(payload == null ? null : payload.toString());
        guia.setFacturadorMensaje(trimToMax(resolveMessage(payload, codigoSunat, mensajeSunat, estadoInterno), 500));
        guia.setFacturadorEndpoint("/guias");
        guia.setFacturadorTipoComprobante(trimToMax(firstNonBlank(tipoDocumento, "09"), 30));

        Integer callbackHttpStatus = readInt(payload, "httpStatus", "statusCode", "status");
        if (callbackHttpStatus != null && callbackHttpStatus > 0) {
            guia.setFacturadorHttpStatus(callbackHttpStatus);
        }

        guia.setFacturacionActualizadoEn(OffsetDateTime.now());
        guiaRemisionRepository.save(guia);
    }

    private String resolveTipoDocumentoByCallbackChannel(String callbackChannel) {
        if (callbackChannel == null) {
            return null;
        }
        return switch (callbackChannel) {
            case "GUIAS" -> "09";
            case "NOTAS_CREDITO" -> "07";
            case "NOTAS_DEBITO" -> "08";
            default -> null;
        };
    }

    private String normalizeCallbackChannel(String callbackChannel) {
        String raw = firstNonBlank(callbackChannel);
        if (raw == null) {
            return "DOCUMENTOS";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private boolean shouldIgnoreMissingVenta(String externalId, String tipoDocumento) {
        if (externalId != null && externalId.toUpperCase(Locale.ROOT).startsWith("GUIA-")) {
            return true;
        }

        if (tipoDocumento == null) {
            return false;
        }

        return "07".equals(tipoDocumento)
                || "08".equals(tipoDocumento)
                || "09".equals(tipoDocumento)
                || "RC".equalsIgnoreCase(tipoDocumento);
    }

    private String resolveEndpointByTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null) {
            return "/documentos";
        }
        return switch (tipoDocumento) {
            case "09" -> "/guias";
            case "07" -> "/notas-credito";
            case "08" -> "/notas-debito";
            default -> "/documentos";
        };
    }

    private Integer readInt(JsonNode source, String... keys) {
        JsonNode node = deepFindNode(source, keys);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.canConvertToInt()) {
            return node.asInt();
        }
        String raw = node.asText("").trim();
        if (raw.isBlank() || !raw.matches("^[0-9]{3}$")) {
            return null;
        }
        return Integer.parseInt(raw);
    }

    private String normalizeTipoDocumento(String raw) {
        String normalized = firstNonBlank(raw);
        if (normalized == null) {
            return null;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "FACTURA", "01" -> "01";
            case "BOLETA", "03" -> "03";
            case "NOTA_CREDITO", "NOTA-CREDITO", "07" -> "07";
            case "NOTA_DEBITO", "NOTA-DEBITO", "08" -> "08";
            case "GUIA", "GUIA_REMISION", "09" -> "09";
            case "TICKET", "TICKET_VENTA", "TK" -> "TK";
            default -> upper;
        };
    }

    private String normalizeEstado(String raw) {
        String normalized = firstNonBlank(raw);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "ACEPTADO", "ACCEPTED", "PROCESADO" -> Venta.FACTURACION_ESTADO_ACEPTADO;
            case "RECHAZADO", "REJECTED" -> Venta.FACTURACION_ESTADO_RECHAZADO;
            case "ERROR", "FAILED" -> Venta.FACTURACION_ESTADO_ERROR;
            case "EN_PROCESO", "PROCESANDO", "PROCESSING" -> Venta.FACTURACION_ESTADO_PROCESANDO;
            case "RECIBIDO", "REGISTERED", "REGISTRADO", "PENDIENTE", "PENDING", "EN_COLA", "QUEUED", "RECEIVED"
                    -> Venta.FACTURACION_ESTADO_PENDIENTE;
            default -> normalized.trim().toUpperCase(Locale.ROOT);
        };
    }

    private String normalizeUrl(String value) {
        String normalized = firstNonBlank(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.regionMatches(true, 0, "http://", 0, 7)
                || normalized.regionMatches(true, 0, "https://", 0, 8)) {
            return trimToMax(normalized, 500);
        }
        return null;
    }

    private String readText(JsonNode source, String... keys) {
        JsonNode node = deepFindNode(source, keys);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return firstNonBlank(node.asText(null));
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

    public record CallbackProcessResult(
            Long ventaId,
            String tenantId,
            String externalId,
            String estado,
            String callbackChannel,
            String tipoDocumento,
            VentaStatusRealtimeEvent realtimeEvent
    ) {
    }
}
