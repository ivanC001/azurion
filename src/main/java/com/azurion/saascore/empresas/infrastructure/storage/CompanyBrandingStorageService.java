package com.azurion.saascore.empresas.infrastructure.storage;

import com.azurion.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CompanyBrandingStorageService {

    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> JPEG_EXTENSIONS = Set.of(".jpg", ".jpeg");

    private final Path rootDirectory;

    public CompanyBrandingStorageService(
            @Value("${azurion.storage.public-files.root-dir:${user.dir}/storage/public-files}") String rootDirectory
    ) {
        this.rootDirectory = Paths.get(rootDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureWritableStorage() {
        try {
            Files.createDirectories(rootDirectory);
            Path probe = Files.createTempFile(rootDirectory, ".azurion-write-test-", ".tmp");
            Files.deleteIfExists(probe);
            log.info("Almacenamiento publico listo en {}", rootDirectory);
        } catch (IOException ex) {
            log.error("El almacenamiento publico no es escribible en {}: {}", rootDirectory, ex.getMessage());
            throw new IllegalStateException(
                    "AZURION_PUBLIC_FILES_DIR no existe o no permite escritura: " + rootDirectory,
                    ex
            );
        }
    }

    public String storePanelLogo(String tenantId, MultipartFile file) {
        try {
            byte[] content = file == null ? new byte[0] : file.getBytes();
            LogoType logoType = validateLogo(file, content);
            String safeTenant = sanitizeSegment(tenantId);
            String filename = "panel-logo-" + UUID.randomUUID() + logoType.extension();
            Path relativePath = Paths.get("company-branding", safeTenant, filename);
            Path targetPath = rootDirectory.resolve(relativePath).normalize();
            if (!targetPath.startsWith(rootDirectory)) {
                throw new BusinessException("EMPRESA_LOGO_PATH_INVALID", "La ruta del logo no es valida.");
            }

            Files.createDirectories(targetPath.getParent());
            Path temporary = Files.createTempFile(targetPath.getParent(), "logo-", ".tmp");
            try {
                Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    Files.move(temporary, targetPath, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return relativePath.toString().replace('\\', '/');
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("No se pudo guardar logo de tenant={} en {}: {}", tenantId, rootDirectory, ex.getMessage());
            throw new BusinessException("EMPRESA_LOGO_SAVE_ERROR", "No se pudo guardar el logo del panel.");
        }
    }

    private LogoType validateLogo(MultipartFile file, byte[] content) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPRESA_LOGO_REQUIRED", "Debes adjuntar un logo valido para el panel.");
        }
        if (content.length == 0 || content.length > MAX_LOGO_BYTES) {
            throw new BusinessException("EMPRESA_LOGO_TOO_LARGE", "El logo del panel no debe superar 2 MB.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String mimeType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        LogoType logoType = detectLogoType(content);
        boolean extensionMatches = extension.equals(logoType.extension())
                || (logoType == LogoType.JPEG && JPEG_EXTENSIONS.contains(extension));
        if (!extensionMatches || (!mimeType.isBlank() && !logoType.mimeTypes().contains(mimeType))) {
            throw new BusinessException("EMPRESA_LOGO_INVALID", "El contenido del logo debe ser PNG, JPG o WEBP valido.");
        }
        return logoType;
    }

    private String extractExtension(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.trim().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private LogoType detectLogoType(byte[] content) {
        if (content.length >= 8 && Arrays.equals(
                Arrays.copyOf(content, 8),
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        )) {
            return LogoType.PNG;
        }
        if (content.length >= 3 && content[0] == (byte) 0xff && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff) {
            return LogoType.JPEG;
        }
        if (content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return LogoType.WEBP;
        }
        throw new BusinessException("EMPRESA_LOGO_INVALID", "El archivo no contiene una imagen PNG, JPG o WEBP valida.");
    }

    private String sanitizeSegment(String value) {
        String normalized = value == null ? "empresa" : value.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9_-]", "-").replaceAll("-{2,}", "-");
        sanitized = sanitized.replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "empresa" : sanitized;
    }

    private enum LogoType {
        PNG(".png", Set.of("image/png")),
        JPEG(".jpg", Set.of("image/jpeg", "image/jpg")),
        WEBP(".webp", Set.of("image/webp"));

        private final String extension;
        private final Set<String> mimeTypes;

        LogoType(String extension, Set<String> mimeTypes) {
            this.extension = extension;
            this.mimeTypes = mimeTypes;
        }

        String extension() {
            return extension;
        }

        Set<String> mimeTypes() {
            return mimeTypes;
        }
    }
}
