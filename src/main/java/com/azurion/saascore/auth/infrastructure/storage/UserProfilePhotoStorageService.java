package com.azurion.saascore.auth.infrastructure.storage;

import com.azurion.shared.exception.BusinessException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfilePhotoStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserProfilePhotoStorageService.class);
    private static final long MAX_PHOTO_BYTES = 1024L * 1024L;
    private static final Set<String> JPEG_EXTENSIONS = Set.of(".jpg", ".jpeg");

    private final Path rootDirectory;

    public UserProfilePhotoStorageService(
            @Value("${azurion.storage.public-files.root-dir:${user.dir}/storage/public-files}") String rootDirectory
    ) {
        this.rootDirectory = Paths.get(rootDirectory).toAbsolutePath().normalize();
    }

    public String store(String tenantId, Long userId, MultipartFile file) {
        try {
            byte[] content = file == null ? new byte[0] : file.getBytes();
            PhotoType type = validate(file, content);
            Path relativePath = Paths.get(
                    "user-profiles",
                    sanitizeSegment(tenantId),
                    "user-" + userId + "-" + UUID.randomUUID() + type.extension()
            );
            Path target = rootDirectory.resolve(relativePath).normalize();
            ensureInsideStorage(target);
            Files.createDirectories(target.getParent());

            Path temporary = Files.createTempFile(target.getParent(), "profile-", ".tmp");
            try {
                Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return "/files/" + relativePath.toString().replace('\\', '/');
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            log.error("No se pudo guardar la foto del perfil userId={}, tenant={}: {}", userId, tenantId, exception.getMessage());
            throw new BusinessException("PERFIL_FOTO_SAVE_ERROR", "No se pudo guardar la foto del perfil.");
        }
    }

    public void deleteQuietly(String publicUrl) {
        Path path = resolvePublicUrl(publicUrl);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("No se pudo eliminar una foto de perfil reemplazada: {}", path);
        }
    }

    private PhotoType validate(MultipartFile file, byte[] content) {
        if (file == null || file.isEmpty() || content.length == 0) {
            throw new BusinessException("PERFIL_FOTO_REQUERIDA", "Selecciona una foto para tu perfil.");
        }
        if (content.length > MAX_PHOTO_BYTES) {
            throw new BusinessException("PERFIL_FOTO_MUY_GRANDE", "La foto del perfil no debe superar 1 MB.");
        }

        String extension = extension(file.getOriginalFilename());
        String mimeType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        PhotoType detected = detect(content);
        boolean extensionMatches = extension.equals(detected.extension())
                || (detected == PhotoType.JPEG && JPEG_EXTENSIONS.contains(extension));
        if (!extensionMatches || (!mimeType.isBlank() && !detected.mimeTypes().contains(mimeType))) {
            throw new BusinessException("PERFIL_FOTO_INVALIDA", "La foto debe ser una imagen PNG, JPG o WEBP valida.");
        }
        return detected;
    }

    private PhotoType detect(byte[] content) {
        if (content.length >= 8 && Arrays.equals(
                Arrays.copyOf(content, 8),
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        )) {
            return PhotoType.PNG;
        }
        if (content.length >= 3 && content[0] == (byte) 0xff && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff) {
            return PhotoType.JPEG;
        }
        if (content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return PhotoType.WEBP;
        }
        throw new BusinessException("PERFIL_FOTO_INVALIDA", "El archivo no contiene una imagen PNG, JPG o WEBP valida.");
    }

    private Path resolvePublicUrl(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/files/user-profiles/")) {
            return null;
        }
        Path path = rootDirectory.resolve(publicUrl.substring("/files/".length())).normalize();
        ensureInsideStorage(path);
        return path;
    }

    private void ensureInsideStorage(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new BusinessException("PERFIL_FOTO_PATH_INVALID", "La ruta de la foto no es valida.");
        }
    }

    private String extension(String filename) {
        String name = filename == null ? "" : filename.trim().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }

    private String sanitizeSegment(String value) {
        String normalized = value == null ? "tenant" : value.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9_-]", "-").replaceAll("-{2,}", "-");
        sanitized = sanitized.replaceAll("^-+|-+$", "");
        return sanitized.isBlank() ? "tenant" : sanitized;
    }

    private enum PhotoType {
        PNG(".png", Set.of("image/png")),
        JPEG(".jpg", Set.of("image/jpeg", "image/jpg")),
        WEBP(".webp", Set.of("image/webp"));

        private final String extension;
        private final Set<String> mimeTypes;

        PhotoType(String extension, Set<String> mimeTypes) {
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
