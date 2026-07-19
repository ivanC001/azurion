package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.dto.UpdateEmpresaBrandingRequest;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.empresas.infrastructure.storage.CompanyBrandingStorageService;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        } else if (request.getLogoPanelUrl() != null && !request.getLogoPanelUrl().isBlank()) {
            throw new BusinessException(
                    "EMPRESA_LOGO_URL_NOT_ALLOWED",
                    "Por seguridad, el logo debe cargarse como archivo y no desde una URL externa."
            );
        }

        Empresa saved = empresaRepository.save(empresa);
        return getCurrentEmpresaUseCase.toResponse(saved);
    }

    private String buildPublicFileUrl(String relativePath) {
        return "/files/" + relativePath;
    }
}
