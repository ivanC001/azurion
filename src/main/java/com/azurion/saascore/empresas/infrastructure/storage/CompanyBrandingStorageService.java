package com.azurion.saascore.empresas.infrastructure.storage;

import com.azurion.shared.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyBrandingStorageService {

    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".svg");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/svg+xml"
    );

    private final Path rootDirectory;

    public CompanyBrandingStorageService(
            @Value("${azurion.storage.public-files.root-dir:${user.dir}/storage/public-files}") String rootDirectory
    ) {
        this.rootDirectory = Paths.get(rootDirectory).toAbsolutePath().normalize();
    }

    public String storePanelLogo(String tenantId, MultipartFile file) {
        validateLogo(file);

        String safeTenant = sanitizeSegment(tenantId);
        String extension = resolveStorageExtension(file.getOriginalFilename());
        String filename = "panel-logo-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT)
                .format(OffsetDateTime.now()) + extension;
        Path relativePath = Paths.get("company-branding", safeTenant, filename);
        Path targetPath = rootDirectory.resolve(relativePath).normalize();

        if (!targetPath.startsWith(rootDirectory)) {
            throw new BusinessException("EMPRESA_LOGO_PATH_INVALID", "La ruta del logo no es valida.");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException("EMPRESA_LOGO_SAVE_ERROR", "No se pudo guardar el logo del panel.");
        }

        return relativePath.toString().replace('\\', '/');
    }

    public boolean isLikelyExternalUrl(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private void validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPRESA_LOGO_REQUIRED", "Debes adjuntar un logo valido para el panel.");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("EMPRESA_LOGO_TOO_LARGE", "El logo del panel no debe superar 2 MB.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String mimeType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension) || (!mimeType.isBlank() && !ALLOWED_MIME_TYPES.contains(mimeType))) {
            throw new BusinessException("EMPRESA_LOGO_INVALID", "El logo del panel debe ser PNG, JPG, WEBP o SVG.");
        }
    }

    private String extractExtension(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.trim().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private String resolveStorageExtension(String originalFilename) {
        String extension = extractExtension(originalFilename);
        return ALLOWED_EXTENSIONS.contains(extension) ? extension : ".png";
    }

    private String sanitizeSegment(String value) {
        String normalized = value == null ? "empresa" : value.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9_-]", "-").replaceAll("-{2,}", "-");
        sanitized = sanitized.replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "empresa" : sanitized;
    }
}
