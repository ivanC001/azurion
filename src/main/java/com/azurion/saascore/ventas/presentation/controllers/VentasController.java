package com.azurion.saascore.ventas.presentation.controllers;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.ventas.application.dto.RegisterVentaRequest;
import com.azurion.saascore.ventas.application.dto.FormatoImpresionComprobante;
import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.application.dto.VentaSummaryResponse;
import com.azurion.saascore.ventas.application.usecases.DownloadVentaComprobanteUseCase;
import com.azurion.saascore.ventas.application.usecases.ListVentasUseCase;
import com.azurion.saascore.ventas.application.usecases.RegisterVentaUseCase;
import com.azurion.saascore.caja.application.usecases.RetryVentaFacturacionUseCase;
import com.azurion.saascore.ventas.infrastructure.realtime.VentaStatusRealtimeStreamService;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/saas/ventas")
@RequiredArgsConstructor
@RequireModule({"ERP", "VENTAS"})
public class VentasController {

    private final RegisterVentaUseCase registerVentaUseCase;
    private final ListVentasUseCase listVentasUseCase;
    private final DownloadVentaComprobanteUseCase downloadVentaComprobanteUseCase;
    private final VentaStatusRealtimeStreamService ventaStatusRealtimeStreamService;
    private final RetryVentaFacturacionUseCase retryVentaFacturacionUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ApiResponse<List<VentaResponse>> list(@RequestParam(required = false) String q) {
        return ApiResponse.ok(listVentasUseCase.execute(q), "Ventas");
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ApiResponse<PageResponse<VentaResponse>> page(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(listVentasUseCase.page(q, page, size), "Ventas paginadas");
    }

    @GetMapping("/{ventaId}")
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ApiResponse<VentaResponse> get(@PathVariable Long ventaId) {
        return ApiResponse.ok(listVentasUseCase.get(ventaId), "Venta");
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ApiResponse<VentaSummaryResponse> summary() {
        return ApiResponse.ok(listVentasUseCase.summary(), "Resumen de ventas");
    }

    @GetMapping(value = "/{ventaId}/comprobante/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long ventaId,
            @RequestParam(defaultValue = "A4") FormatoImpresionComprobante formato
    ) {
        var artifact = downloadVentaComprobanteUseCase.execute(ventaId, formato);
        String disposition = ContentDisposition.inline()
                .filename(artifact.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(artifact.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                .body(artifact.content());
    }

    @GetMapping(value = "/{ventaId}/comprobante/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ResponseEntity<byte[]> downloadXml(@PathVariable Long ventaId) {
        return attachment(downloadVentaComprobanteUseCase.executeXml(ventaId));
    }

    @GetMapping(value = "/{ventaId}/comprobante/cdr", produces = "application/zip")
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ResponseEntity<byte[]> downloadCdr(@PathVariable Long ventaId) {
        return attachment(downloadVentaComprobanteUseCase.executeCdr(ventaId));
    }

    private ResponseEntity<byte[]> attachment(
            FacturadorClient.FacturadorArtifactDownload artifact
    ) {
        String disposition = ContentDisposition.attachment()
                .filename(artifact.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(artifact.contentType()))
                .contentLength(artifact.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                .body(artifact.content());
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public SseEmitter statusEvents() {
        return ventaStatusRealtimeStreamService.subscribe(TenantContext.getTenantId());
    }

    @PostMapping("/{ventaId}/facturacion/reintentar")
    @PreAuthorize("hasAuthority('VENTAS_CREATE')")
    public ApiResponse<VentaResponse> retryDocument(@PathVariable Long ventaId) {
        retryVentaFacturacionUseCase.execute(ventaId);
        return ApiResponse.ok(listVentasUseCase.get(ventaId), "Reintento de generacion programado");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VENTAS_CREATE')")
    public ApiResponse<VentaResponse> register(@Valid @RequestBody RegisterVentaRequest request) {
        return ApiResponse.ok(registerVentaUseCase.execute(request), "Venta registrada");
    }
}
