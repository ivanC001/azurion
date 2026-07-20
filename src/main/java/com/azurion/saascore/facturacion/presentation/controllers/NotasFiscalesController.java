package com.azurion.saascore.facturacion.presentation.controllers;

import com.azurion.saascore.facturacion.application.dto.NotaFiscalResponse;
import com.azurion.saascore.facturacion.application.dto.RegistrarNotaFiscalRequest;
import com.azurion.saascore.facturacion.application.dto.RegistrarNotaFiscalResponse;
import com.azurion.saascore.facturacion.application.usecases.ListNotasFiscalesUseCase;
import com.azurion.saascore.facturacion.application.usecases.RegistrarNotaFiscalUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/notas")
@RequiredArgsConstructor
@RequireModule({"ERP", "FACTURACION"})
public class NotasFiscalesController {

    private final RegistrarNotaFiscalUseCase registrarNotaFiscalUseCase;
    private final ListNotasFiscalesUseCase listNotasFiscalesUseCase;

    @GetMapping("/credito")
    @PreAuthorize("hasAuthority('FACTURACION_READ')")
    public ApiResponse<List<NotaFiscalResponse>> listarCredito(@RequestParam(required = false) String q) {
        return ApiResponse.ok(listNotasFiscalesUseCase.execute("07", q), "Notas de credito");
    }

    @GetMapping("/credito/page")
    @PreAuthorize("hasAuthority('FACTURACION_READ')")
    public ApiResponse<PageResponse<NotaFiscalResponse>> pageCredito(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(listNotasFiscalesUseCase.page("07", q, page, size), "Notas de credito paginadas");
    }

    @PostMapping("/credito")
    @PreAuthorize("hasAuthority('NOTA_CREDITO_CREATE')")
    public ApiResponse<RegistrarNotaFiscalResponse> registrarCredito(@Valid @RequestBody RegistrarNotaFiscalRequest request) {
        return ApiResponse.ok(registrarNotaFiscalUseCase.execute("07", request), "Nota de credito enviada al facturador");
    }

    @GetMapping("/debito")
    @PreAuthorize("hasAuthority('FACTURACION_READ')")
    public ApiResponse<List<NotaFiscalResponse>> listarDebito(@RequestParam(required = false) String q) {
        return ApiResponse.ok(listNotasFiscalesUseCase.execute("08", q), "Notas de debito");
    }

    @GetMapping("/debito/page")
    @PreAuthorize("hasAuthority('FACTURACION_READ')")
    public ApiResponse<PageResponse<NotaFiscalResponse>> pageDebito(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(listNotasFiscalesUseCase.page("08", q, page, size), "Notas de debito paginadas");
    }

    @PostMapping("/debito")
    @PreAuthorize("hasAuthority('NOTA_DEBITO_CREATE')")
    public ApiResponse<RegistrarNotaFiscalResponse> registrarDebito(@Valid @RequestBody RegistrarNotaFiscalRequest request) {
        return ApiResponse.ok(registrarNotaFiscalUseCase.execute("08", request), "Nota de debito enviada al facturador");
    }
}
