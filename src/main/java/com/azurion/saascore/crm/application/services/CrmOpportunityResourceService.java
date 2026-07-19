package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.crm.application.dto.CrmOportunidadRecursoRequest;
import com.azurion.saascore.crm.application.dto.CrmOportunidadRecursoResponse;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmOportunidadRecurso;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRecursoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.infrastructure.storage.CrmPrivateFileStorageService;
import com.azurion.saascore.crm.infrastructure.storage.CrmPrivateFileStorageService.StoredFile;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CrmOpportunityResourceService {

    private static final Set<String> TYPES = Set.of("REQUISITO", "PAGO", "DOCUMENTO", "CIERRE");
    private static final Set<String> PAYMENT_TYPES = Set.of("FACTURA", "BOLETA", "TICKET", "VOUCHER", "CUOTA", "OTRO");
    private static final Set<String> PAYMENT_STATES = Set.of("PENDIENTE", "PARCIAL", "PAGADO", "VENCIDO");
    private static final Set<String> DOCUMENT_CATEGORIES = Set.of("CONTRATO", "PROPUESTA", "PAGO", "LEGAL", "OTRO");
    private static final int MAX_DATA_JSON_LENGTH = 20_000;

    private final CrmOportunidadRepository oportunidadRepository;
    private final CrmOportunidadRecursoRepository recursoRepository;
    private final CrmPrivateFileStorageService fileStorageService;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CrmOportunidadRecursoResponse> listAllScoped() {
        List<CrmOportunidadRecurso> resources = canViewAll()
                ? recursoRepository.findAllByOrderByCreatedAtDescIdDesc()
                : recursoRepository.findByOportunidadResponsableIdOrderByCreatedAtDescIdDesc(currentUserKey());
        return resources.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CrmOportunidadRecursoResponse> list(Long opportunityId) {
        CrmOportunidad opportunity = findOpportunity(opportunityId);
        ensureCanRead(opportunity);
        return recursoRepository.findByOportunidadIdOrderByCreatedAtDescIdDesc(opportunityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CrmOportunidadRecursoResponse create(Long opportunityId,
                                                CrmOportunidadRecursoRequest request,
                                                MultipartFile file) {
        CrmOportunidad opportunity = findOpportunity(opportunityId);
        ensureCanWrite(opportunity);
        String type = normalizeType(request.tipo());
        Map<String, Object> data = validateData(type, request.data());
        String externalKey = externalKey(data);
        if (externalKey != null) {
            var existing = recursoRepository.findByOportunidadIdAndTipoAndExternalKey(opportunityId, type, externalKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        if ("PAGO".equals(type) && "PAGADO".equals(String.valueOf(data.get("estado")))
                && (file == null || file.isEmpty())) {
            throw new BusinessException("CRM_PAGO_COMPROBANTE_REQUERIDO", "El pago requiere un voucher o comprobante");
        }

        CrmOportunidadRecurso resource = new CrmOportunidadRecurso();
        resource.setOportunidad(opportunity);
        resource.setTipo(type);
        resource.setExternalKey(externalKey);
        resource.setDataJson(writeData(data));
        resource.setCreatedBy(currentUserKey());
        StoredFile stored = attachNewFile(resource, opportunityId, file);
        try {
            return toResponse(recursoRepository.save(resource));
        } catch (RuntimeException ex) {
            if (stored != null) {
                fileStorageService.deleteQuietly(stored.relativePath());
            }
            throw ex;
        }
    }

    @Transactional
    public CrmOportunidadRecursoResponse update(Long opportunityId,
                                                Long resourceId,
                                                CrmOportunidadRecursoRequest request,
                                                MultipartFile file) {
        CrmOportunidadRecurso resource = findResource(opportunityId, resourceId);
        ensureCanWrite(resource.getOportunidad());
        String type = normalizeType(request.tipo());
        if (!resource.getTipo().equals(type)) {
            throw new BusinessException("CRM_RECURSO_TIPO_INMUTABLE", "No se puede cambiar el tipo del registro");
        }
        Map<String, Object> data = validateData(type, request.data());
        if ("PAGO".equals(type) && "PAGADO".equals(String.valueOf(data.get("estado")))
                && resource.getArchivoPath() == null && (file == null || file.isEmpty())) {
            throw new BusinessException("CRM_PAGO_COMPROBANTE_REQUERIDO", "El pago requiere un voucher o comprobante");
        }
        resource.setDataJson(writeData(data));
        String oldPath = resource.getArchivoPath();
        StoredFile stored = attachNewFile(resource, opportunityId, file);
        try {
            CrmOportunidadRecurso saved = recursoRepository.save(resource);
            if (stored != null && oldPath != null) {
                deleteAfterCommit(oldPath);
            }
            return toResponse(saved);
        } catch (RuntimeException ex) {
            if (stored != null) {
                fileStorageService.deleteQuietly(stored.relativePath());
            }
            throw ex;
        }
    }

    @Transactional
    public void delete(Long opportunityId, Long resourceId) {
        CrmOportunidadRecurso resource = findResource(opportunityId, resourceId);
        ensureCanWrite(resource.getOportunidad());
        recursoRepository.delete(resource);
        deleteAfterCommit(resource.getArchivoPath());
    }

    @Transactional(readOnly = true)
    public ResourceFile download(Long opportunityId, Long resourceId) {
        CrmOportunidadRecurso resource = findResource(opportunityId, resourceId);
        ensureCanRead(resource.getOportunidad());
        if (resource.getArchivoPath() == null) {
            throw new BusinessException("CRM_ARCHIVO_NO_ENCONTRADO", "El registro no tiene un archivo adjunto");
        }
        return new ResourceFile(
                resource.getArchivoNombre(),
                resource.getArchivoMimeType(),
                fileStorageService.read(resource.getArchivoPath())
        );
    }

    private CrmOportunidad findOpportunity(Long id) {
        return oportunidadRepository.findWithRelationsById(id)
                .orElseThrow(() -> new BusinessException("CRM_OPORTUNIDAD_NO_ENCONTRADA", "Oportunidad no encontrada"));
    }

    private CrmOportunidadRecurso findResource(Long opportunityId, Long resourceId) {
        CrmOportunidadRecurso resource = recursoRepository.findWithOportunidadById(resourceId)
                .orElseThrow(() -> new BusinessException("CRM_RECURSO_NO_ENCONTRADO", "Registro de oportunidad no encontrado"));
        if (!resource.getOportunidad().getId().equals(opportunityId)) {
            throw new BusinessException("CRM_RECURSO_NO_ENCONTRADO", "Registro de oportunidad no encontrado");
        }
        return resource;
    }

    private StoredFile attachNewFile(CrmOportunidadRecurso resource, Long opportunityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        StoredFile stored = fileStorageService.store(opportunityId, file);
        resource.setArchivoPath(stored.relativePath());
        resource.setArchivoNombre(stored.originalName());
        resource.setArchivoMimeType(stored.mimeType());
        resource.setArchivoSize(stored.size());
        return stored;
    }

    private Map<String, Object> validateData(String type, Map<String, Object> source) {
        Map<String, Object> data = new LinkedHashMap<>(source == null ? Map.of() : source);
        switch (type) {
            case "REQUISITO" -> {
                requiredString(data, "nombre", 220);
                positiveDecimal(data, "cantidad", false);
                positiveDecimal(data, "precioUnitario", true);
            }
            case "PAGO" -> {
                requiredDate(data, "fecha");
                requireEnum(data, "tipo", PAYMENT_TYPES);
                positiveDecimal(data, "monto", false);
                requireEnum(data, "estado", PAYMENT_STATES);
            }
            case "DOCUMENTO" -> {
                requiredString(data, "nombre", 220);
                requireEnum(data, "categoria", DOCUMENT_CATEGORIES);
            }
            case "CIERRE" -> {
                data.put("closedAt", OffsetDateTime.now().toString());
                data.put("closedBy", currentUserKey());
            }
            default -> throw new BusinessException("CRM_RECURSO_TIPO_INVALIDO", "Tipo de registro no permitido");
        }
        return data;
    }

    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw new BusinessException("CRM_RECURSO_TIPO_INVALIDO", "Tipo de registro no permitido");
        }
        return type;
    }

    private String externalKey(Map<String, Object> data) {
        Object raw = data.get("clientKey");
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank() || value.length() > 180 || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new BusinessException("CRM_RECURSO_CLIENT_KEY_INVALIDO", "La clave de idempotencia del registro no es valida");
        }
        data.put("clientKey", value);
        return value;
    }

    private void requiredString(Map<String, Object> data, String field, int maxLength) {
        String value = String.valueOf(data.getOrDefault(field, "")).trim();
        if (value.isBlank() || value.length() > maxLength) {
            throw new BusinessException("CRM_RECURSO_DATO_INVALIDO", "El campo " + field + " es obligatorio y no puede superar " + maxLength + " caracteres");
        }
        data.put(field, value);
    }

    private void requiredDate(Map<String, Object> data, String field) {
        try {
            data.put(field, LocalDate.parse(String.valueOf(data.get(field))).toString());
        } catch (RuntimeException ex) {
            throw new BusinessException("CRM_RECURSO_DATO_INVALIDO", "El campo " + field + " debe ser una fecha valida");
        }
    }

    private void positiveDecimal(Map<String, Object> data, String field, boolean allowZero) {
        try {
            BigDecimal value = new BigDecimal(String.valueOf(data.get(field)));
            boolean invalid = allowZero ? value.signum() < 0 : value.signum() <= 0;
            if (invalid) {
                throw new NumberFormatException();
            }
            data.put(field, value);
        } catch (RuntimeException ex) {
            throw new BusinessException("CRM_RECURSO_DATO_INVALIDO", "El campo " + field + " tiene un monto invalido");
        }
    }

    private void requireEnum(Map<String, Object> data, String field, Set<String> allowed) {
        String value = String.valueOf(data.getOrDefault(field, "")).trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new BusinessException("CRM_RECURSO_DATO_INVALIDO", "El campo " + field + " contiene un valor no permitido");
        }
        data.put(field, value);
    }

    private String writeData(Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            if (json.length() > MAX_DATA_JSON_LENGTH) {
                throw new BusinessException("CRM_RECURSO_DATOS_MUY_GRANDES", "Los datos del registro son demasiado grandes");
            }
            return json;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("CRM_RECURSO_JSON_INVALIDO", "No se pudieron procesar los datos del registro");
        }
    }

    private Map<String, Object> readData(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException ex) {
            throw new BusinessException("CRM_RECURSO_JSON_INVALIDO", "Los datos guardados del registro no son validos");
        }
    }

    private CrmOportunidadRecursoResponse toResponse(CrmOportunidadRecurso resource) {
        return new CrmOportunidadRecursoResponse(
                resource.getId(),
                resource.getOportunidad().getId(),
                resource.getTipo(),
                readData(resource.getDataJson()),
                resource.getArchivoPath() != null,
                resource.getArchivoNombre(),
                resource.getArchivoMimeType(),
                resource.getArchivoSize(),
                resource.getCreatedBy(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }

    private void ensureCanRead(CrmOportunidad opportunity) {
        if (!canViewAll() && !currentUserKey().equals(opportunity.getResponsableId())) {
            throw new BusinessException("CRM_SIN_ACCESO", "No tienes acceso a este registro CRM");
        }
    }

    private void ensureCanWrite(CrmOportunidad opportunity) {
        ensureCanRead(opportunity);
    }

    private boolean canViewAll() {
        return hasAuthority("CRM_VIEW_ALL")
                || hasAuthority("ROLE_ADMIN_GENERAL")
                || hasAuthority("ROLE_PLATFORM_ADMIN");
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private String currentUserKey() {
        Long userId = authorizationService.currentUsuarioId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "system" : authentication.getName();
    }

    private void deleteAfterCommit(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.deleteQuietly(relativePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.deleteQuietly(relativePath);
            }
        });
    }

    public record ResourceFile(String name, String mimeType, byte[] content) {
    }
}
