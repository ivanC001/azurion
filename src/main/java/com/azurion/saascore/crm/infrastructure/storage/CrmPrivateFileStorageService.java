package com.azurion.saascore.crm.infrastructure.storage;

import com.azurion.multitenancy.TenantContext;
import com.azurion.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CrmPrivateFileStorageService {

    public static final long MAX_FILE_BYTES = 8L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".png", ".jpg", ".jpeg", ".webp", ".txt", ".doc", ".docx", ".xls", ".xlsx"
    );
    private static final Map<String, String> OFFICE_MIME_TYPES = Map.of(
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".doc", "application/msword",
            ".xls", "application/vnd.ms-excel"
    );

    private final Path rootDirectory;

    public CrmPrivateFileStorageService(
            @Value("${azurion.storage.private-files.root-dir:${user.dir}/storage/private-files}") String rootDirectory
    ) {
        this.rootDirectory = Paths.get(rootDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initializeStorage() {
        Path probe = null;
        try {
            Files.createDirectories(rootDirectory);
            if (!Files.isDirectory(rootDirectory)) {
                throw new IOException("Configured path is not a directory");
            }
            probe = Files.createTempFile(rootDirectory, ".crm-storage-check-", ".tmp");
            log.info("CRM private file storage ready at {}", rootDirectory);
        } catch (IOException | SecurityException ex) {
            log.error("CRM private file storage is not writable at {}", rootDirectory, ex);
            throw new IllegalStateException(
                    "CRM private file storage is not writable. Check AZURION_PRIVATE_FILES_DIR and volume permissions: "
                            + rootDirectory,
                    ex
            );
        } finally {
            if (probe != null) {
                try {
                    Files.deleteIfExists(probe);
                } catch (IOException ex) {
                    log.warn("Could not remove CRM storage probe file {}", probe, ex);
                }
            }
        }
    }

    public StoredFile store(Long opportunityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("CRM_ARCHIVO_REQUERIDO", "Debes adjuntar un archivo valido");
        }
        try {
            byte[] content = file.getBytes();
            if (content.length == 0 || content.length > MAX_FILE_BYTES) {
                throw new BusinessException("CRM_ARCHIVO_TAMANIO_INVALIDO", "El archivo debe pesar como maximo 8 MB");
            }
            String originalName = safeOriginalName(file.getOriginalFilename());
            String extension = extension(originalName);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BusinessException("CRM_ARCHIVO_TIPO_INVALIDO", "Tipo de archivo no permitido");
            }
            String detectedMime = validateContent(extension, content);
            String tenant = sanitizeSegment(TenantContext.getTenantId());
            Path relative = Paths.get("crm", tenant, String.valueOf(opportunityId), UUID.randomUUID() + extension);
            Path target = rootDirectory.resolve(relative).normalize();
            if (!target.startsWith(rootDirectory)) {
                throw new BusinessException("CRM_ARCHIVO_PATH_INVALIDO", "La ruta del archivo no es valida");
            }
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
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
            return new StoredFile(relative.toString().replace('\\', '/'), originalName, detectedMime, (long) content.length);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | SecurityException ex) {
            log.error(
                    "Could not store CRM private file. root={}, opportunityId={}, tenant={}, originalName={}, size={}",
                    rootDirectory,
                    opportunityId,
                    TenantContext.getTenantId(),
                    file == null ? null : safeOriginalName(file.getOriginalFilename()),
                    file == null ? 0 : file.getSize(),
                    ex
            );
            throw new BusinessException("CRM_ARCHIVO_SAVE_ERROR", "No se pudo guardar el archivo");
        }
    }

    public byte[] read(String relativePath) {
        Path path = resolveManagedPath(relativePath);
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_FILE_BYTES) {
                throw new BusinessException("CRM_ARCHIVO_NO_ENCONTRADO", "El archivo no existe");
            }
            return Files.readAllBytes(path);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("CRM_ARCHIVO_READ_ERROR", "No se pudo leer el archivo");
        }
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveManagedPath(relativePath));
        } catch (IOException ignored) {
            // A maintenance job may clean orphaned private files later.
        }
    }

    private Path resolveManagedPath(String relativePath) {
        Path resolved = rootDirectory.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new BusinessException("CRM_ARCHIVO_PATH_INVALIDO", "La ruta del archivo no es valida");
        }
        return resolved;
    }

    private String validateContent(String extension, byte[] content) {
        if (".pdf".equals(extension) && startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            return "application/pdf";
        }
        if (".png".equals(extension) && startsWith(content, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {
            return "image/png";
        }
        if ((".jpg".equals(extension) || ".jpeg".equals(extension))
                && content.length >= 3 && content[0] == (byte) 0xff && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff) {
            return "image/jpeg";
        }
        if (".webp".equals(extension) && content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return "image/webp";
        }
        if ((".docx".equals(extension) || ".xlsx".equals(extension))
                && content.length >= 4 && content[0] == 'P' && content[1] == 'K') {
            return OFFICE_MIME_TYPES.get(extension);
        }
        if ((".doc".equals(extension) || ".xls".equals(extension))
                && startsWith(content, new byte[]{(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1})) {
            return OFFICE_MIME_TYPES.get(extension);
        }
        if (".txt".equals(extension) && isUtf8Text(content)) {
            return "text/plain";
        }
        throw new BusinessException("CRM_ARCHIVO_CONTENIDO_INVALIDO", "El contenido no coincide con la extension del archivo");
    }

    private boolean isUtf8Text(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException ignored) {
            return false;
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private String safeOriginalName(String originalName) {
        String normalized = originalName == null ? "archivo" : originalName.replace('\\', '/');
        int lastSeparator = normalized.lastIndexOf('/');
        String name = lastSeparator >= 0 ? normalized.substring(lastSeparator + 1) : normalized;
        name = name.replaceAll("[\\r\\n\\\"]", "_").trim();
        return name.isBlank() ? "archivo" : name.substring(0, Math.min(name.length(), 255));
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String sanitizeSegment(String value) {
        String normalized = value == null ? "public" : value.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9_-]", "-").replaceAll("-{2,}", "-");
        return sanitized.isBlank() ? "public" : sanitized;
    }

    public record StoredFile(String relativePath, String originalName, String mimeType, long size) {
    }
}
