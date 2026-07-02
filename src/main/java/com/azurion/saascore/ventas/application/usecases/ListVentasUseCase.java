package com.azurion.saascore.ventas.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListVentasUseCase {

    private final VentaRepository ventaRepository;
    private final EmpresaRepository empresaRepository;
    private final FacturadorClient facturadorClient;

    @Transactional(readOnly = true)
    public List<VentaResponse> execute(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();

        List<Venta> ventas = ventaRepository.findAllByOrderByFechaVentaDesc().stream()
                .filter(venta -> normalized.isBlank() || matches(venta, normalized))
                .toList();

        Map<String, FacturadorClient.FacturadorDocumentoStatusResult> statusByExternalId = consultarEstadosFacturador(ventas);

        return ventas.stream()
                .map(venta -> toResponse(venta, statusByExternalId.get(venta.getExternalId())))
                .toList();
    }

    private boolean matches(Venta venta, String query) {
        return contains(venta.getExternalId(), query)
                || contains(venta.getClienteNombre(), query)
                || contains(venta.getClienteDocumento(), query)
                || contains(venta.getMoneda(), query)
                || contains(venta.getFacturacionEstado(), query)
                || contains(venta.getFacturadorSunatEstado(), query)
                || contains(venta.getFacturadorMensaje(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private Map<String, FacturadorClient.FacturadorDocumentoStatusResult> consultarEstadosFacturador(List<Venta> ventas) {
        if (ventas.isEmpty()) {
            return Map.of();
        }

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || "public".equalsIgnoreCase(tenantId)) {
            return Map.of();
        }

        Empresa empresa = empresaRepository.findByTenantId(tenantId).orElse(null);
        if (empresa == null || empresa.getRuc() == null || empresa.getRuc().isBlank()) {
            return Map.of();
        }

        List<String> externalIds = ventas.stream()
                .filter(venta -> !Venta.FACTURACION_ESTADO_NO_REQUIERE.equalsIgnoreCase(venta.getFacturacionEstado()))
                .map(Venta::getExternalId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .limit(150)
                .toList();

        if (externalIds.isEmpty()) {
            return Map.of();
        }

        try {
            return facturadorClient.consultarDocumentosPorExternalIds(tenantId, empresa.getRuc(), externalIds);
        } catch (Exception exception) {
            log.warn("No se pudo consultar estados en facturador para tenant {}: {}", tenantId, exception.getMessage());
            return Map.of();
        }
    }

    private VentaResponse toResponse(Venta venta, FacturadorClient.FacturadorDocumentoStatusResult facturador) {
        String localEstado = normalizeEstado(venta.getFacturacionEstado());
        String remoteEstado = normalizeEstado(facturador == null ? null : facturador.estadoInterno());
        String localSunatEstado = normalizeEstado(venta.getFacturadorSunatEstado());
        String remoteSunatEstado = normalizeEstado(facturador == null ? null : facturador.sunatEstado());

        String sunatEstado = firstNonBlank(remoteSunatEstado, localSunatEstado, remoteEstado, localEstado);
        String estadoFinal = resolveEstadoFinal(localEstado, remoteEstado, localSunatEstado, remoteSunatEstado);

        Integer httpStatus = venta.getFacturadorHttpStatus() != null
                ? venta.getFacturadorHttpStatus()
                : (facturador == null ? null : facturador.httpStatus());

        String mensajeFacturador = firstNonBlank(
                preferRemoteMessage(estadoFinal, facturador == null ? null : facturador.sunatMensaje()),
                venta.getFacturadorMensaje(),
                facturador == null ? null : facturador.sunatMensaje(),
                facturador == null ? null : facturador.estadoInterno()
        );

        String rawJson = firstNonBlank(
                venta.getFacturadorRespuestaJson(),
                facturador == null || facturador.rawData() == null ? null : facturador.rawData().toString()
        );

        return new VentaResponse(
                venta.getId(),
                venta.getExternalId(),
                venta.getClienteDocumento(),
                venta.getClienteNombre(),
                venta.getMoneda(),
                venta.getTotal(),
                venta.getFechaVenta(),
                estadoFinal,
                venta.getFacturacionIntentos(),
                httpStatus,
                firstNonBlank(venta.getFacturadorEndpoint(), facturador == null ? null : "/documentos"),
                firstNonBlank(venta.getFacturadorTipoComprobante(), facturador == null ? null : facturador.tipoDocumento()),
                mensajeFacturador,
                sunatEstado,
                firstNonBlank(venta.getFacturadorDocumentoId(), facturador == null || facturador.documentoId() == null ? null : String.valueOf(facturador.documentoId())),
                firstNonBlank(venta.getFacturadorTicket(), facturador == null ? null : facturador.ticket()),
                firstNonBlank(venta.getFacturadorPdfUrl(), facturador == null ? null : facturador.pdfUrl()),
                firstNonBlank(venta.getFacturadorXmlUrl(), facturador == null ? null : facturador.xmlUrl()),
                firstNonBlank(venta.getFacturadorCdrUrl(), facturador == null ? null : facturador.cdrUrl()),
                rawJson,
                venta.getFacturacionActualizadoEn()
        );
    }

    private String resolveEstadoFinal(
            String localEstado,
            String remoteEstado,
            String localSunatEstado,
            String remoteSunatEstado
    ) {
        if (isTerminal(remoteSunatEstado)) {
            return remoteSunatEstado;
        }
        if (isTerminal(remoteEstado)) {
            return remoteEstado;
        }
        if (isTerminal(localSunatEstado)) {
            return localSunatEstado;
        }
        if (isTerminal(localEstado)) {
            return localEstado;
        }

        String moving = firstNonBlank(remoteSunatEstado, remoteEstado, localSunatEstado, localEstado);
        if (moving == null || moving.isBlank()) {
            return Venta.FACTURACION_ESTADO_PENDIENTE;
        }
        return moving;
    }

    private String preferRemoteMessage(String estadoFinal, String remoteMessage) {
        if (!isTerminal(estadoFinal)) {
            return null;
        }
        return remoteMessage;
    }

    private boolean isTerminal(String estado) {
        return Venta.FACTURACION_ESTADO_ACEPTADO.equals(estado)
                || Venta.FACTURACION_ESTADO_RECHAZADO.equals(estado)
                || Venta.FACTURACION_ESTADO_ERROR.equals(estado)
                || Venta.FACTURACION_ESTADO_NO_REQUIERE.equals(estado);
    }

    private String normalizeEstado(String raw) {
        String normalized = firstNonBlank(raw);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase()) {
            case "ACEPTADO", "ACCEPTED" -> Venta.FACTURACION_ESTADO_ACEPTADO;
            case "RECHAZADO", "REJECTED" -> Venta.FACTURACION_ESTADO_RECHAZADO;
            case "ERROR", "FAILED" -> Venta.FACTURACION_ESTADO_ERROR;
            case "EN_PROCESO", "PROCESANDO", "PROCESSING" -> Venta.FACTURACION_ESTADO_PROCESANDO;
            case "RECIBIDO", "REGISTERED", "REGISTRADO", "PENDIENTE", "PENDING", "EN_COLA", "QUEUED" -> Venta.FACTURACION_ESTADO_PENDIENTE;
            default -> normalized.toUpperCase();
        };
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }
}
