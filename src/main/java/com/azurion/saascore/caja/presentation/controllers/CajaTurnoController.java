package com.azurion.saascore.caja.presentation.controllers;

import com.azurion.saascore.caja.application.dto.AbrirCajaTurnoRequest;
import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.dto.CajaTurnoResponse;
import com.azurion.saascore.caja.application.dto.CerrarCajaTurnoRequest;
import com.azurion.saascore.caja.application.dto.DepositoCuentaEmpresarialRequest;
import com.azurion.saascore.caja.application.dto.RegistrarMovimientoCajaRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaResponse;
import com.azurion.saascore.caja.application.services.CajaTurnoService;
import com.azurion.saascore.caja.application.usecases.DepositarCuentaEmpresarialUseCase;
import com.azurion.saascore.caja.application.usecases.ListCajaMovimientosUseCase;
import com.azurion.saascore.caja.application.usecases.RegistrarMovimientoCajaUseCase;
import com.azurion.saascore.caja.application.usecases.RegistrarVentaCajaUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/caja-turnos", "/v1/saas/cajas"})
@RequiredArgsConstructor
@RequireModule({"ERP", "CAJA"})
public class CajaTurnoController {

    private final CajaTurnoService cajaTurnoService;
    private final RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase;
    private final DepositarCuentaEmpresarialUseCase depositarCuentaEmpresarialUseCase;
    private final ListCajaMovimientosUseCase listCajaMovimientosUseCase;
    private final RegistrarVentaCajaUseCase registrarVentaCajaUseCase;

    @PostMapping("/abrir")
    @PreAuthorize("hasAuthority('CAJA_OPEN')")
    public ApiResponse<CajaTurnoResponse> open(
            @Valid @RequestBody AbrirCajaTurnoRequest request) {
        return ApiResponse.ok(cajaTurnoService.open(request), "Turno de caja abierto");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<List<CajaTurnoResponse>> list(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long sucursalId) {
        return ApiResponse.ok(cajaTurnoService.list(estado, sucursalId), "Turnos de caja");
    }

    @GetMapping("/activo")
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<CajaTurnoResponse> active() {
        return ApiResponse.ok(
                cajaTurnoService.activeForCurrentUser().orElse(null),
                "Turno activo del usuario"
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<CajaTurnoResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(cajaTurnoService.get(id), "Turno de caja");
    }

    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('CAJA_MOVEMENT_CREATE')")
    public ApiResponse<CajaMovimientoResponse> movement(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarMovimientoCajaRequest request) {
        return ApiResponse.ok(registrarMovimientoCajaUseCase.execute(id, request), "Movimiento registrado");
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<List<CajaMovimientoResponse>> movements(@PathVariable Long id) {
        return ApiResponse.ok(listCajaMovimientosUseCase.execute(id), "Movimientos del turno");
    }

    @PostMapping("/{id}/depositos-cuenta-empresarial")
    @PreAuthorize("hasAuthority('CAJA_DEPOSIT')")
    public ApiResponse<CajaMovimientoResponse> deposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositoCuentaEmpresarialRequest request) {
        return ApiResponse.ok(
                depositarCuentaEmpresarialUseCase.execute(id, request),
                "Deposito registrado"
        );
    }

    @PostMapping("/{id}/ventas")
    @PreAuthorize("hasAuthority('VENTAS_CREATE')")
    @RequireModule("VENTAS")
    public ApiResponse<RegistrarVentaCajaResponse> sale(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarVentaCajaRequest request) {
        return ApiResponse.ok(
                registrarVentaCajaUseCase.execute(id, request),
                "Venta registrada. Facturacion en proceso"
        );
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('CAJA_CLOSE')")
    public ApiResponse<CajaTurnoResponse> close(
            @PathVariable Long id,
            @Valid @RequestBody CerrarCajaTurnoRequest request) {
        return ApiResponse.ok(cajaTurnoService.close(id, request), "Turno de caja cerrado");
    }
}
