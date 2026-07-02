package com.azurion.saascore.cotizaciones.presentation.controllers;

import com.azurion.saascore.cotizaciones.application.dto.ConvertCotizacionVentaRequest;
import com.azurion.saascore.cotizaciones.application.dto.ConvertCotizacionVentaResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.CreateCotizacionRequest;
import com.azurion.saascore.cotizaciones.application.dto.CreatePromocionCotizacionRequest;
import com.azurion.saascore.cotizaciones.application.dto.PromocionCotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.application.usecases.ConvertCotizacionVentaUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.CreateCotizacionUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.GenerateCotizacionPdfUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.GetCotizacionUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.ListCotizacionesUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.PromocionCotizacionUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.UpdateCotizacionEstadoUseCase;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/cotizaciones", "/cotizaciones"})
@RequiredArgsConstructor
@RequireModule("COTIZACIONES")
public class CotizacionController {

    private final ListCotizacionesUseCase listCotizacionesUseCase;
    private final GetCotizacionUseCase getCotizacionUseCase;
    private final CreateCotizacionUseCase createCotizacionUseCase;
    private final UpdateCotizacionEstadoUseCase updateCotizacionEstadoUseCase;
    private final GenerateCotizacionPdfUseCase generateCotizacionPdfUseCase;
    private final ConvertCotizacionVentaUseCase convertCotizacionVentaUseCase;
    private final PromocionCotizacionUseCase promocionCotizacionUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('COTIZACIONES_READ')")
    public ApiResponse<List<CotizacionResponse>> list(@RequestParam(required = false) Long crmOportunidadId) {
        return ApiResponse.ok(listCotizacionesUseCase.execute(crmOportunidadId), "Cotizaciones");
    }

    @GetMapping("/promociones")
    @PreAuthorize("hasAuthority('COTIZACIONES_READ')")
    public ApiResponse<List<PromocionCotizacionResponse>> listPromociones() {
        return ApiResponse.ok(promocionCotizacionUseCase.list(), "Promociones de cotizacion");
    }

    @PostMapping("/promociones")
    @PreAuthorize("hasAuthority('COTIZACIONES_CREATE')")
    public ApiResponse<PromocionCotizacionResponse> createPromocion(@Valid @RequestBody CreatePromocionCotizacionRequest request) {
        return ApiResponse.ok(promocionCotizacionUseCase.create(request), "Promocion registrada");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COTIZACIONES_READ')")
    public ApiResponse<CotizacionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(getCotizacionUseCase.execute(id), "Cotizacion");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COTIZACIONES_CREATE')")
    public ApiResponse<CotizacionResponse> create(@Valid @RequestBody CreateCotizacionRequest request) {
        return ApiResponse.ok(createCotizacionUseCase.execute(request), "Cotizacion registrada");
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('COTIZACIONES_UPDATE')")
    public ApiResponse<CotizacionResponse> updateEstado(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateCotizacionEstadoRequest request) {
        return ApiResponse.ok(updateCotizacionEstadoUseCase.execute(id, request), "Estado de cotizacion actualizado");
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('COTIZACIONES_PDF')")
    public ApiResponse<CotizacionPdfResponse> pdf(@PathVariable Long id) {
        return ApiResponse.ok(generateCotizacionPdfUseCase.execute(id), "PDF de cotizacion generado");
    }

    @PostMapping("/{id}/convertir-venta")
    @PreAuthorize("hasAuthority('COTIZACIONES_CONVERT_SALE')")
    public ApiResponse<ConvertCotizacionVentaResponse> convertirVenta(@PathVariable Long id,
                                                                      @Valid @RequestBody ConvertCotizacionVentaRequest request) {
        return ApiResponse.ok(convertCotizacionVentaUseCase.execute(id, request), "Cotizacion convertida en venta");
    }
}
