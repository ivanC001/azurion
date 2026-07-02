package com.azurion.saascore.planes.presentation.controllers;

import com.azurion.saascore.planes.application.dto.CreatePlanRequest;
import com.azurion.saascore.planes.application.dto.PlanResponse;
import com.azurion.saascore.planes.application.dto.UpdatePlanRequest;
import com.azurion.saascore.planes.application.usecases.CreatePlanUseCase;
import com.azurion.saascore.planes.application.usecases.GetPlanByIdUseCase;
import com.azurion.saascore.planes.application.usecases.ListPlanesUseCase;
import com.azurion.saascore.planes.application.usecases.UpdatePlanUseCase;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/planes", "/admin/planes"})
@RequiredArgsConstructor
public class PlanController {

    private final CreatePlanUseCase createPlanUseCase;
    private final ListPlanesUseCase listPlanesUseCase;
    private final GetPlanByIdUseCase getPlanByIdUseCase;
    private final UpdatePlanUseCase updatePlanUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<PlanResponse> create(@Valid @RequestBody CreatePlanRequest request) {
        return ApiResponse.ok(createPlanUseCase.execute(request), "Plan creado");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<List<PlanResponse>> list() {
        return ApiResponse.ok(listPlanesUseCase.execute(), "Planes");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<PlanResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(getPlanByIdUseCase.execute(id), "Plan");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN_GENERAL')")
    public ApiResponse<PlanResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdatePlanRequest request) {
        return ApiResponse.ok(updatePlanUseCase.execute(id, request), "Plan actualizado");
    }
}
