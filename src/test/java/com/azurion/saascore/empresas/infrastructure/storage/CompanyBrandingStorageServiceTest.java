package com.azurion.saascore.empresas.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.shared.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class CompanyBrandingStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesDetectedImageWithGeneratedNameInsideRoot() throws Exception {
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", png);
        CompanyBrandingStorageService service = new CompanyBrandingStorageService(temporaryDirectory.toString());

        String relative = service.storePanelLogo("Tenant/../../evil", file);

        assertThat(relative).startsWith("company-branding/tenant-evil/panel-logo-").endsWith(".png");
        Path stored = temporaryDirectory.resolve(relative).normalize();
        assertThat(stored).startsWith(temporaryDirectory).exists();
        assertThat(Files.readAllBytes(stored)).isEqualTo(png);
    }

    @Test
    void rejectsSvgAndExtensionSpoofing() {
        CompanyBrandingStorageService service = new CompanyBrandingStorageService(temporaryDirectory.toString());
        MockMultipartFile svg = new MockMultipartFile(
                "file", "logo.svg", "image/svg+xml", "<svg onload=alert(1)></svg>".getBytes()
        );
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile disguised = new MockMultipartFile("file", "logo.jpg", "image/jpeg", png);

        assertThatThrownBy(() -> service.storePanelLogo("tenant", svg))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.storePanelLogo("tenant", disguised))
                .isInstanceOf(BusinessException.class);
    }
}
