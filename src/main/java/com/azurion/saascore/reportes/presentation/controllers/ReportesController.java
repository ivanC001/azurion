package com.azurion.saascore.reportes.presentation.controllers;

import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.saascore.reportes.application.dto.FiscalSummaryResponse;
import com.azurion.saascore.reportes.application.usecases.GetFiscalSummaryUseCase;
import com.azurion.shared.api.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/reportes")
@RequiredArgsConstructor
@RequireModule({"ERP", "VENTAS", "INVENTARIO"})
public class ReportesController {

    private final GetFiscalSummaryUseCase getFiscalSummaryUseCase;

    @GetMapping("/fiscal")
    @PreAuthorize("hasAnyAuthority('VENTAS_READ','INVENTORY_READ')")
    public ApiResponse<FiscalSummaryResponse> fiscal(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ApiResponse.ok(
                getFiscalSummaryUseCase.execute(desde, hasta),
                "Resumen fiscal y margen real"
        );
    }
}
