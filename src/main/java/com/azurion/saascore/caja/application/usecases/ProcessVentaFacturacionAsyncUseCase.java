package com.azurion.saascore.caja.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.caja.application.dto.VentaFacturacionAsyncTask;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.ventas.application.dto.VentaStatusRealtimeEvent;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.saascore.ventas.infrastructure.realtime.VentaStatusRealtimeStreamService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessVentaFacturacionAsyncUseCase {

    private final FacturadorClient facturadorClient;
    private final VentaRepository ventaRepository;
    private final PlatformTransactionManager transactionManager;
    private final VentaStatusRealtimeStreamService ventaStatusRealtimeStreamService;

    public void execute(VentaFacturacionAsyncTask task) {
        TenantContext.setTenantId(task.tenantId());
        try {
            runInNewTransaction(() -> markVentaAsProcessing(task));

            FacturadorClient.FacturadorEmissionResult emission = facturadorClient.emitirDocumento(
                    task.tenantId(),
                    task.tenantRuc(),
                    task.endpoint(),
                    task.payload(),
                    task.tipoComprobante()
            );

            runInNewTransaction(() -> applyEmissionResult(task, emission));
            if (!emission.success()) {
                throw new IllegalStateException(trimToMax(emission.message(), 500));
            }
        } catch (Exception exception) {
            log.error("Error procesando facturacion async para venta {}", task.externalId(), exception);
            try {
                runInNewTransaction(() -> markVentaAsError(task, exception.getMessage()));
            } catch (Exception markError) {
                log.error("No se pudo marcar la venta {} como error", task.externalId(), markError);
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Fallo el procesamiento de facturacion", exception);
        } finally {
            TenantContext.clear();
        }
    }

    private void markVentaAsProcessing(VentaFacturacionAsyncTask task) {
        Venta venta = requireVenta(task);
        venta.setFacturacionEstado(Venta.FACTURACION_ESTADO_PROCESANDO);
        venta.setFacturadorEndpoint(task.endpoint());
        venta.setFacturadorTipoComprobante(task.tipoComprobante());
        venta.setFacturacionIntentos(nextIntento(venta.getFacturacionIntentos()));
        venta.setFacturadorMensaje("Enviando comprobante al facturador");
        venta.setFacturacionActualizadoEn(OffsetDateTime.now());
        ventaRepository.save(venta);
        emitVentaStatusAfterCommit(VentaStatusRealtimeEvent.fromVenta(task.tenantId(), "ASYNC_PROCESSING", venta));
    }

    private void applyEmissionResult(VentaFacturacionAsyncTask task, FacturadorClient.FacturadorEmissionResult emission) {
        Venta venta = requireVenta(task);
        JsonNode responseBody = emission.responseBody();
        venta.setFacturadorHttpStatus(emission.status());
        venta.setFacturadorMensaje(trimToMax(emission.message(), 500));
        venta.setFacturadorSunatEstado(readUpperText(responseBody, "estado", "sunat_estado", "estado_sunat", "status"));
        venta.setFacturadorDocumentoId(readText(responseBody, "documento_id", "id_documento", "documentId"));
        venta.setFacturadorTicket(readText(responseBody, "ticket", "ticket_sunat"));
        venta.setFacturadorPdfUrl(readUrl(responseBody, "pdf_url", "url_pdf", "pdf"));
        venta.setFacturadorXmlUrl(readUrl(responseBody, "xml_url", "url_xml", "xml"));
        venta.setFacturadorCdrUrl(readUrl(responseBody, "cdr_url", "url_cdr", "cdr"));
        venta.setFacturadorRespuestaJson(responseBody == null ? null : responseBody.toString());
        venta.setFacturacionEstado(resolveFacturacionEstado(task.tipoComprobante(), venta.getFacturadorSunatEstado(), emission.success()));
        venta.setFacturacionActualizadoEn(OffsetDateTime.now());
        ventaRepository.save(venta);
        emitVentaStatusAfterCommit(VentaStatusRealtimeEvent.fromVenta(task.tenantId(), "ASYNC_RESULT", venta));
    }

    private Venta requireVenta(VentaFacturacionAsyncTask task) {
        return ventaRepository.findById(task.ventaId())
                .or(() -> ventaRepository.findByExternalId(task.externalId()))
                .orElseThrow(() -> new IllegalStateException("No se encontro la venta " + task.externalId()));
    }

    private void markVentaAsError(VentaFacturacionAsyncTask task, String message) {
        Venta venta = ventaRepository.findById(task.ventaId())
                .or(() -> ventaRepository.findByExternalId(task.externalId()))
                .orElse(null);

        if (venta == null) {
            return;
        }

        venta.setFacturacionEstado(Venta.FACTURACION_ESTADO_ERROR);
        venta.setFacturadorEndpoint(task.endpoint());
        venta.setFacturadorTipoComprobante(task.tipoComprobante());
        venta.setFacturadorMensaje(trimToMax(message, 500));
        venta.setFacturacionIntentos(venta.getFacturacionIntentos() == null ? 1 : venta.getFacturacionIntentos());
        venta.setFacturacionActualizadoEn(OffsetDateTime.now());
        ventaRepository.save(venta);
        emitVentaStatusAfterCommit(VentaStatusRealtimeEvent.fromVenta(task.tenantId(), "ASYNC_ERROR", venta));
    }

    private Integer nextIntento(Integer intentosActuales) {
        return intentosActuales == null ? 1 : intentosActuales + 1;
    }

    private String resolveFacturacionEstado(String tipoComprobante, String sunatEstado, boolean success) {
        if ("TICKET_VENTA".equalsIgnoreCase(tipoComprobante)) {
            return success ? Venta.FACTURACION_ESTADO_ACEPTADO : Venta.FACTURACION_ESTADO_ERROR;
        }

        if (sunatEstado == null || sunatEstado.isBlank()) {
            return success ? Venta.FACTURACION_ESTADO_PROCESANDO : Venta.FACTURACION_ESTADO_ERROR;
        }
        return switch (sunatEstado) {
            case Venta.FACTURACION_ESTADO_ACEPTADO -> Venta.FACTURACION_ESTADO_ACEPTADO;
            case Venta.FACTURACION_ESTADO_RECHAZADO -> Venta.FACTURACION_ESTADO_RECHAZADO;
            case Venta.FACTURACION_ESTADO_ERROR -> Venta.FACTURACION_ESTADO_ERROR;
            default -> Venta.FACTURACION_ESTADO_PROCESANDO;
        };
    }

    private String readUpperText(JsonNode source, String... keys) {
        String value = readText(source, keys);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

    private void runInNewTransaction(Runnable action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private void emitVentaStatusAfterCommit(VentaStatusRealtimeEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            ventaStatusRealtimeStreamService.publish(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ventaStatusRealtimeStreamService.publish(event);
            }
        });
    }
}
