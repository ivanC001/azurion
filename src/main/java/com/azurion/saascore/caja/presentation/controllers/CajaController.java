package com.azurion.saascore.caja.presentation.controllers;

import com.azurion.saascore.caja.application.dto.AbrirCajaRequest;
import com.azurion.saascore.caja.application.dto.CajaMovimientoResponse;
import com.azurion.saascore.caja.application.dto.CajaResponse;
import com.azurion.saascore.caja.application.dto.CerrarCajaRequest;
import com.azurion.saascore.caja.application.dto.DepositoCuentaEmpresarialRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaResponse;
import com.azurion.saascore.caja.application.dto.RegistrarMovimientoCajaRequest;
import com.azurion.saascore.caja.application.usecases.AbrirCajaUseCase;
import com.azurion.saascore.caja.application.usecases.CerrarCajaUseCase;
import com.azurion.saascore.caja.application.usecases.DepositarCuentaEmpresarialUseCase;
import com.azurion.saascore.caja.application.usecases.GetCajaByIdUseCase;
import com.azurion.saascore.caja.application.usecases.ListCajaMovimientosUseCase;
import com.azurion.saascore.caja.application.usecases.ListCajasUseCase;
import com.azurion.saascore.caja.application.usecases.RegistrarVentaCajaUseCase;
import com.azurion.saascore.caja.application.usecases.RegistrarMovimientoCajaUseCase;
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
@RequestMapping("/v1/saas/cajas")
@RequiredArgsConstructor
@RequireModule({"ERP", "CAJA"})
public class CajaController {

    private final AbrirCajaUseCase abrirCajaUseCase;
    private final CerrarCajaUseCase cerrarCajaUseCase;
    private final RegistrarMovimientoCajaUseCase registrarMovimientoCajaUseCase;
    private final DepositarCuentaEmpresarialUseCase depositarCuentaEmpresarialUseCase;
    private final ListCajasUseCase listCajasUseCase;
    private final GetCajaByIdUseCase getCajaByIdUseCase;
    private final ListCajaMovimientosUseCase listCajaMovimientosUseCase;
    private final RegistrarVentaCajaUseCase registrarVentaCajaUseCase;

    @PostMapping("/abrir")
    @PreAuthorize("hasAuthority('CAJA_OPEN')")
    public ApiResponse<CajaResponse> abrir(@Valid @RequestBody AbrirCajaRequest request) {
        return ApiResponse.ok(abrirCajaUseCase.execute(request), "Caja abierta");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<List<CajaResponse>> list(@RequestParam(required = false) String estado,
                                                @RequestParam(required = false) Long sucursalId) {
        return ApiResponse.ok(listCajasUseCase.execute(estado, sucursalId), "Cajas");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<CajaResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getCajaByIdUseCase.execute(id), "Caja");
    }

    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('CAJA_MOVEMENT_CREATE')")
    public ApiResponse<CajaMovimientoResponse> movimiento(@PathVariable Long id,
                                                          @Valid @RequestBody RegistrarMovimientoCajaRequest request) {
        return ApiResponse.ok(registrarMovimientoCajaUseCase.execute(id, request), "Movimiento registrado");
    }

    @PostMapping("/{id}/ventas")
    @PreAuthorize("hasAuthority('VENTAS_CREATE')")
    @RequireModule("VENTAS")
    public ApiResponse<RegistrarVentaCajaResponse> registrarVenta(@PathVariable Long id,
                                                                  @Valid @RequestBody RegistrarVentaCajaRequest request) {
        return ApiResponse.ok(registrarVentaCajaUseCase.execute(id, request), "Venta registrada. Facturacion en proceso");
    }

    @PostMapping("/{id}/depositos-cuenta-empresarial")
    @PreAuthorize("hasAuthority('CAJA_DEPOSIT')")
    public ApiResponse<CajaMovimientoResponse> depositoCuenta(@PathVariable Long id,
                                                              @Valid @RequestBody DepositoCuentaEmpresarialRequest request) {
        return ApiResponse.ok(depositarCuentaEmpresarialUseCase.execute(id, request), "Deposito registrado");
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('CAJA_CLOSE')")
    public ApiResponse<CajaResponse> cerrar(@PathVariable Long id,
                                            @Valid @RequestBody CerrarCajaRequest request) {
        return ApiResponse.ok(cerrarCajaUseCase.execute(id, request), "Caja cerrada");
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('CAJA_READ')")
    public ApiResponse<List<CajaMovimientoResponse>> movimientos(@PathVariable Long id) {
        return ApiResponse.ok(listCajaMovimientosUseCase.execute(id), "Movimientos de caja");
    }
}
