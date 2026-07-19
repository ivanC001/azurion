package com.azurion.saascore.ventas.presentation.controllers;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.saascore.ventas.application.dto.RegisterVentaRequest;
import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.application.usecases.ListVentasUseCase;
import com.azurion.saascore.ventas.application.usecases.RegisterVentaUseCase;
import com.azurion.saascore.ventas.infrastructure.realtime.VentaStatusRealtimeStreamService;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final VentaStatusRealtimeStreamService ventaStatusRealtimeStreamService;

    @GetMapping
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public ApiResponse<List<VentaResponse>> list(@RequestParam(required = false) String q) {
        return ApiResponse.ok(listVentasUseCase.execute(q), "Ventas");
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('VENTAS_READ')")
    public SseEmitter statusEvents() {
        return ventaStatusRealtimeStreamService.subscribe(TenantContext.getTenantId());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VENTAS_CREATE')")
    public ApiResponse<VentaResponse> register(@Valid @RequestBody RegisterVentaRequest request) {
        return ApiResponse.ok(registerVentaUseCase.execute(request), "Venta registrada");
    }
}
