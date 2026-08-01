package com.azurion.saascore.ventas.application.usecases;

import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.application.dto.VentaSummaryResponse;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListVentasUseCase {

    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public List<VentaResponse> execute(String query) {
        // Compatibility endpoint: it is intentionally bounded. Consumers that
        // need the complete history must traverse /page explicitly.
        return page(query, 0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<VentaResponse> page(String query, int page, int size) {
        String normalized = query == null ? "" : query.trim();
        var result = ventaRepository.search(
                normalized,
                PageRequestSupport.of(page, size, Sort.by("fechaVenta").descending().and(Sort.by("id").descending()))
        );
        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public VentaSummaryResponse summary() {
        ZoneId zone = ZoneId.of("America/Lima");
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime dayStart = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return ventaRepository.summarize(dayStart, dayEnd);
    }

    private VentaResponse toResponse(Venta venta) {
        String localEstado = normalizeEstado(venta.getFacturacionEstado());
        String localSunatEstado = normalizeEstado(venta.getFacturadorSunatEstado());

        String sunatEstado = firstNonBlank(localSunatEstado, localEstado);
        String estadoFinal = resolveEstadoFinal(localEstado, null, localSunatEstado, null);

        Integer httpStatus = venta.getFacturadorHttpStatus();
        String mensajeFacturador = venta.getFacturadorMensaje();
        String rawJson = venta.getFacturadorRespuestaJson();

        return new VentaResponse(
                venta.getId(),
                venta.getExternalId(),
                venta.getClienteDocumento(),
                venta.getClienteNombre(),
                venta.getMoneda(),
                venta.getTotal(),
                venta.getCajaTurnoId(),
                venta.getFormaPago(),
                venta.getMetodoPago(),
                venta.getFechaVenta(),
                estadoFinal,
                venta.getFacturacionIntentos(),
                httpStatus,
                venta.getFacturadorEndpoint(),
                venta.getFacturadorTipoComprobante(),
                mensajeFacturador,
                sunatEstado,
                venta.getFacturadorDocumentoId(),
                venta.getFacturadorTicket(),
                venta.getFacturadorPdfUrl(),
                venta.getFacturadorXmlUrl(),
                venta.getFacturadorCdrUrl(),
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
