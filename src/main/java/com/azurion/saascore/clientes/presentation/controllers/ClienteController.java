package com.azurion.saascore.clientes.presentation.controllers;

import com.azurion.saascore.clientes.application.dto.ClienteAbonoResponse;
import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.application.dto.CreateClienteRequest;
import com.azurion.saascore.clientes.application.dto.RegistrarClienteAbonoRequest;
import com.azurion.saascore.clientes.application.dto.UpdateClienteRequest;
import com.azurion.saascore.clientes.application.usecases.CreateClienteUseCase;
import com.azurion.saascore.clientes.application.usecases.DeleteClienteUseCase;
import com.azurion.saascore.clientes.application.usecases.GetClienteByIdUseCase;
import com.azurion.saascore.clientes.application.usecases.ListClientesUseCase;
import com.azurion.saascore.clientes.application.usecases.ListClienteAbonosUseCase;
import com.azurion.saascore.clientes.application.usecases.RegistrarClienteAbonoUseCase;
import com.azurion.saascore.clientes.application.usecases.UpdateClienteUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/v1/saas/clientes")
@RequiredArgsConstructor
@RequireModule("CLIENTES")
public class ClienteController {

    private final CreateClienteUseCase createClienteUseCase;
    private final ListClientesUseCase listClientesUseCase;
    private final GetClienteByIdUseCase getClienteByIdUseCase;
    private final UpdateClienteUseCase updateClienteUseCase;
    private final DeleteClienteUseCase deleteClienteUseCase;
    private final RegistrarClienteAbonoUseCase registrarClienteAbonoUseCase;
    private final ListClienteAbonosUseCase listClienteAbonosUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTES_WRITE')")
    public ApiResponse<ClienteResponse> create(@Valid @RequestBody CreateClienteRequest request) {
        return ApiResponse.ok(createClienteUseCase.execute(request), "Cliente creado");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTES_READ')")
    public ApiResponse<List<ClienteResponse>> list() {
        return ApiResponse.ok(listClientesUseCase.execute(), "Clientes");
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('CLIENTES_READ')")
    public ApiResponse<PageResponse<ClienteResponse>> page(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(listClientesUseCase.page(q, page, size), "Clientes paginados");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_READ')")
    public ApiResponse<ClienteResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getClienteByIdUseCase.execute(id), "Cliente");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_WRITE')")
    public ApiResponse<ClienteResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateClienteRequest request) {
        return ApiResponse.ok(updateClienteUseCase.execute(id, request), "Cliente actualizado");
    }

    @PostMapping("/{id}/abonos")
    @PreAuthorize("hasAuthority('CLIENTES_REGISTER_PAYMENT')")
    public ApiResponse<ClienteAbonoResponse> registrarAbono(@PathVariable Long id,
                                                            @Valid @RequestBody RegistrarClienteAbonoRequest request) {
        return ApiResponse.ok(registrarClienteAbonoUseCase.execute(id, request), "Abono registrado");
    }

    @GetMapping("/{id}/abonos")
    @PreAuthorize("hasAuthority('CLIENTES_VIEW_DEBT')")
    public ApiResponse<List<ClienteAbonoResponse>> listAbonos(@PathVariable Long id) {
        return ApiResponse.ok(listClienteAbonosUseCase.execute(id), "Abonos del cliente");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_WRITE')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        deleteClienteUseCase.execute(id);
        return ApiResponse.ok("OK", "Cliente eliminado");
    }
}
