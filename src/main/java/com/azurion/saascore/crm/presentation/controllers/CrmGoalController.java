package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.CrmMetaResponse;
import com.azurion.saascore.crm.application.dto.SaveCrmMetaRequest;
import com.azurion.saascore.crm.application.services.CrmGoalService;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/crm/metas")
@RequireModule("CRM")
@RequiredArgsConstructor
public class CrmGoalController {

    private final CrmGoalService goalService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CRM_GOALS_READ','CRM_GOALS_MANAGE','CRM_REPORTS_TEAM',"
            + "'ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public ApiResponse<List<CrmMetaResponse>> list(
            @RequestParam Integer anio,
            @RequestParam Integer mes
    ) {
        return ApiResponse.ok(goalService.list(anio, mes), "Metas CRM del periodo");
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CRM_GOALS_MANAGE','ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public ApiResponse<CrmMetaResponse> save(@Valid @RequestBody SaveCrmMetaRequest request) {
        return ApiResponse.ok(goalService.save(request), "Meta CRM guardada");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('CRM_GOALS_MANAGE','ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public void delete(@PathVariable Long id) {
        goalService.delete(id);
    }
}
