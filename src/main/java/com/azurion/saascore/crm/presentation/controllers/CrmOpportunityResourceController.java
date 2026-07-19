package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.CrmOportunidadRecursoRequest;
import com.azurion.saascore.crm.application.dto.CrmOportunidadRecursoResponse;
import com.azurion.saascore.crm.application.services.CrmOpportunityResourceService;
import com.azurion.saascore.crm.application.services.CrmOpportunityResourceService.ResourceFile;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/v1/saas/crm/oportunidades", "/crm/oportunidades"})
@RequiredArgsConstructor
@RequireModule("CRM")
public class CrmOpportunityResourceController {

    private final CrmOpportunityResourceService resourceService;

    @GetMapping("/recursos")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmOportunidadRecursoResponse>> listAll() {
        return ApiResponse.ok(resourceService.listAllScoped(), "Registros de oportunidades CRM");
    }

    @GetMapping("/{opportunityId}/recursos")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmOportunidadRecursoResponse>> list(@PathVariable Long opportunityId) {
        return ApiResponse.ok(resourceService.list(opportunityId), "Registros de la oportunidad CRM");
    }

    @PostMapping(value = "/{opportunityId}/recursos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_WRITE')")
    public ApiResponse<CrmOportunidadRecursoResponse> create(
            @PathVariable Long opportunityId,
            @Valid @RequestPart("metadata") CrmOportunidadRecursoRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.ok(resourceService.create(opportunityId, request, file), "Registro de oportunidad creado");
    }

    @PutMapping(value = "/{opportunityId}/recursos/{resourceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_WRITE')")
    public ApiResponse<CrmOportunidadRecursoResponse> update(
            @PathVariable Long opportunityId,
            @PathVariable Long resourceId,
            @Valid @RequestPart("metadata") CrmOportunidadRecursoRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.ok(resourceService.update(opportunityId, resourceId, request, file), "Registro de oportunidad actualizado");
    }

    @DeleteMapping("/{opportunityId}/recursos/{resourceId}")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_WRITE')")
    public ApiResponse<String> delete(@PathVariable Long opportunityId, @PathVariable Long resourceId) {
        resourceService.delete(opportunityId, resourceId);
        return ApiResponse.ok("OK", "Registro de oportunidad eliminado");
    }

    @GetMapping("/{opportunityId}/recursos/{resourceId}/archivo")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long opportunityId,
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "false") boolean inline
    ) {
        ResourceFile file = resourceService.download(opportunityId, resourceId);
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.name(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.content().length)
                .body(new ByteArrayResource(file.content()));
    }
}
