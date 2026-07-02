package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicCrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/public/crm", "/public/crm"})
@RequiredArgsConstructor
@RequireModule("CRM")
public class PublicCrmLeadController {

    private final CrmUseCaseService crmUseCaseService;

    @PostMapping("/leads")
    public ApiResponse<CrmProspectoResponse> capture(@Valid @RequestBody PublicCrmLeadRequest request) {
        return ApiResponse.ok(crmUseCaseService.capturePublicLead(request), "Lead CRM registrado");
    }

    @GetMapping("/catalogo/{id}")
    public ApiResponse<PublicCrmCatalogoItemResponse> catalogo(@PathVariable Long id,
                                                               @RequestParam String token) {
        return ApiResponse.ok(crmUseCaseService.getPublicCatalogoItem(id, token), "Oferta CRM publica");
    }
}
