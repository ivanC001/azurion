package com.azurion.saascore.auth.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.shared.exception.BusinessException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class UserProfilePhotoStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesValidatedPhotoInsideThePublicProfileDirectory() throws Exception {
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", png);
        UserProfilePhotoStorageService service = new UserProfilePhotoStorageService(temporaryDirectory.toString());

        String publicUrl = service.store("Tenant/../../evil", 42L, file);

        assertThat(publicUrl).startsWith("/files/user-profiles/tenant-evil/user-42-").endsWith(".png");
        Path stored = temporaryDirectory.resolve(publicUrl.substring("/files/".length())).normalize();
        assertThat(stored).startsWith(temporaryDirectory).exists();
        assertThat(Files.readAllBytes(stored)).isEqualTo(png);
    }

    @Test
    void rejectsExtensionSpoofingAndFilesLargerThanOneMegabyte() {
        UserProfilePhotoStorageService service = new UserProfilePhotoStorageService(temporaryDirectory.toString());
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile disguised = new MockMultipartFile("file", "profile.jpg", "image/jpeg", png);

        byte[] oversizedPng = new byte[1024 * 1024 + 1];
        System.arraycopy(png, 0, oversizedPng, 0, png.length);
        MockMultipartFile oversized = new MockMultipartFile("file", "profile.png", "image/png", oversizedPng);

        assertThatThrownBy(() -> service.store("tenant", 1L, disguised))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.store("tenant", 1L, oversized))
                .isInstanceOf(BusinessException.class);
    }
}
