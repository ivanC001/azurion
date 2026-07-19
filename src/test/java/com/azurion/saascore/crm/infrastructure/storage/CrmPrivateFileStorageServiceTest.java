package com.azurion.saascore.crm.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.multitenancy.TenantContext;
import com.azurion.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class CrmPrivateFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void initializesAndStoresPdfInsideSanitizedTenantDirectory() throws Exception {
        TenantContext.setTenantId("Tenant/../../Produccion");
        CrmPrivateFileStorageService service = new CrmPrivateFileStorageService(temporaryDirectory.toString());
        service.initializeStorage();
        byte[] content = "%PDF-1.7\nproduction-test".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "SUNAT - Menu SOL.pdf",
                "application/pdf",
                content
        );

        CrmPrivateFileStorageService.StoredFile stored = service.store(42L, file);

        assertThat(stored.relativePath()).startsWith("crm/tenant-produccion/42/").endsWith(".pdf");
        assertThat(stored.originalName()).isEqualTo("SUNAT - Menu SOL.pdf");
        assertThat(stored.mimeType()).isEqualTo("application/pdf");
        assertThat(service.read(stored.relativePath())).isEqualTo(content);
        assertThat(temporaryDirectory.resolve(stored.relativePath())).exists();
    }

    @Test
    void rejectsPdfWhoseContentDoesNotMatchItsExtension() {
        CrmPrivateFileStorageService service = new CrmPrivateFileStorageService(temporaryDirectory.toString());
        service.initializeStorage();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voucher.pdf",
                "application/pdf",
                "not-a-pdf".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.store(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contenido");
    }

    @Test
    void failsFastWhenConfiguredStorageRootIsAFile() throws Exception {
        Path invalidRoot = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(invalidRoot, "occupied");
        CrmPrivateFileStorageService service = new CrmPrivateFileStorageService(invalidRoot.toString());

        assertThatThrownBy(service::initializeStorage)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURION_PRIVATE_FILES_DIR");
    }
}
