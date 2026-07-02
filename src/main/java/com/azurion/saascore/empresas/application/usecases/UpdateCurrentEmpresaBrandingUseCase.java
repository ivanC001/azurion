package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.dto.UpdateEmpresaBrandingRequest;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.empresas.infrastructure.storage.CompanyBrandingStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class UpdateCurrentEmpresaBrandingUseCase {

    private final EmpresaRepository empresaRepository;
    private final GetCurrentEmpresaUseCase getCurrentEmpresaUseCase;
    private final CompanyBrandingStorageService storageService;

    public EmpresaResponse execute(UpdateEmpresaBrandingRequest request) {
        Empresa empresa = getCurrentEmpresaUseCase.resolveCurrentEmpresa();

        if (Boolean.TRUE.equals(request.getClearLogoPanel())) {
            empresa.setLogoPanelUrl(null);
        } else if (request.getLogoPanelFile() != null && !request.getLogoPanelFile().isEmpty()) {
            String relativePath = storageService.storePanelLogo(empresa.getTenantId(), request.getLogoPanelFile());
            empresa.setLogoPanelUrl(buildPublicFileUrl(relativePath));
        } else if (storageService.isLikelyExternalUrl(request.getLogoPanelUrl())) {
            empresa.setLogoPanelUrl(request.getLogoPanelUrl().trim());
        }

        Empresa saved = empresaRepository.save(empresa);
        return getCurrentEmpresaUseCase.toResponse(saved);
    }

    private String buildPublicFileUrl(String relativePath) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files/")
                .path(relativePath)
                .toUriString();
    }
}
