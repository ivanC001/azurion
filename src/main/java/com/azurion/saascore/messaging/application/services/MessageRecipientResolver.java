package com.azurion.saascore.messaging.application.services;

import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.messaging.domain.entities.MessageAudience;
import com.azurion.saascore.messaging.domain.entities.MessageRecipientScope;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioGlobalRolRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageRecipientResolver {

    private static final String PUBLIC_TENANT = "public";
    private static final Set<String> PLATFORM_ADMIN_ROLES = Set.of("ADMIN_GENERAL", "PLATFORM_ADMIN");

    private final UsuarioGlobalRepository globalUserRepository;
    private final UsuarioGlobalRolRepository globalRoleRepository;
    private final EmpresaRepository empresaRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<RecipientTarget> resolve(
            MessageAudience audience,
            String tenantId,
            List<Long> selectedUserIds
    ) {
        Map<String, RecipientTarget> recipients = new LinkedHashMap<>();
        switch (audience) {
            case PLATFORM_ADMINS -> addAll(recipients, globalUsers(true));
            case TENANT_ADMINS -> addAll(recipients, tenantUsers(requireTenant(tenantId), true, List.of()));
            case TENANT_USERS -> addAll(recipients, tenantUsers(requireTenant(tenantId), false, List.of()));
            case SELECTED_USERS -> addAll(
                    recipients,
                    tenantUsers(requireTenant(tenantId), false, requireSelectedUsers(selectedUserIds))
            );
            case ALL_USERS -> {
                addAll(recipients, globalUsers(false));
                empresaRepository.findByActivoTrueOrderByRazonSocialAsc()
                        .forEach(empresa -> addAll(
                                recipients,
                                tenantUsers(empresa.getTenantId(), false, List.of())
                        ));
            }
        }
        if (recipients.isEmpty()) {
            throw new BusinessException(
                    "MESSAGE_AUDIENCE_EMPTY",
                    "La audiencia seleccionada no tiene usuarios activos."
            );
        }
        return List.copyOf(recipients.values());
    }

    private List<RecipientTarget> globalUsers(boolean administratorsOnly) {
        List<RecipientTarget> result = new ArrayList<>();
        for (UsuarioGlobal user : globalUserRepository.findByActivoTrueOrderByUsernameAsc()) {
            if (administratorsOnly && !isPlatformAdmin(user)) {
                continue;
            }
            result.add(new RecipientTarget(
                    MessageRecipientScope.PLATFORM,
                    PUBLIC_TENANT,
                    user.getId(),
                    user.getUsername(),
                    user.getUsername()
            ));
        }
        return result;
    }

    private boolean isPlatformAdmin(UsuarioGlobal user) {
        Set<String> roles = Arrays.stream((user.getRoles() == null ? "" : user.getRoles()).split(","))
                .map(this::normalizeRole)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        globalRoleRepository.findByUsuarioGlobalIdAndActivoTrue(user.getId())
                .forEach(assignment -> roles.add(normalizeRole(assignment.getRolCodigo())));
        return roles.stream().anyMatch(PLATFORM_ADMIN_ROLES::contains);
    }

    private List<RecipientTarget> tenantUsers(String tenantId, boolean administratorsOnly, List<Long> selectedIds) {
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .filter(Empresa::isActivo)
                .orElseThrow(() -> new BusinessException(
                        "MESSAGE_TENANT_NOT_FOUND",
                        "El tenant seleccionado no existe o esta inactivo."
                ));
        String schema = safeSchema(empresa.getSchemaName());
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT usuario.id, usuario.username, usuario.nombres
                  FROM "%s".usuarios usuario
                """.formatted(schema));
        List<Object> parameters = new ArrayList<>();
        if (administratorsOnly) {
            sql.append("""
                     JOIN "%s".usuario_roles assignment ON assignment.usuario_id = usuario.id
                     JOIN "%s".roles role ON role.id = assignment.rol_id
                    """.formatted(schema, schema));
        }
        sql.append(" WHERE usuario.activo = TRUE");
        if (administratorsOnly) {
            sql.append(" AND role.codigo IN ('ADMIN', 'ADMIN_EMPRESA')");
        }
        if (!selectedIds.isEmpty()) {
            sql.append(" AND usuario.id IN (");
            sql.append(String.join(",", java.util.Collections.nCopies(selectedIds.size(), "?")));
            sql.append(")");
            parameters.addAll(selectedIds);
        }
        sql.append(" ORDER BY usuario.nombres, usuario.id");

        List<RecipientTarget> targets = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new RecipientTarget(
                        MessageRecipientScope.TENANT,
                        tenantId,
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("nombres")
                ),
                parameters.toArray()
        );
        if (!selectedIds.isEmpty()) {
            Set<Long> found = targets.stream().map(RecipientTarget::userId).collect(Collectors.toSet());
            if (found.size() != selectedIds.size()) {
                throw new BusinessException(
                        "MESSAGE_RECIPIENT_INVALID",
                        "Uno o mas usuarios seleccionados no existen, estan inactivos o no pertenecen al tenant."
                );
            }
        }
        return targets;
    }

    private List<Long> requireSelectedUsers(List<Long> selectedUserIds) {
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            throw new BusinessException(
                    "MESSAGE_RECIPIENTS_REQUIRED",
                    "Selecciona al menos un usuario para enviar el mensaje."
            );
        }
        List<Long> normalized = selectedUserIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (normalized.size() != selectedUserIds.size()) {
            throw new BusinessException(
                    "MESSAGE_RECIPIENTS_INVALID",
                    "La seleccion contiene usuarios repetidos o identificadores invalidos."
            );
        }
        return normalized;
    }

    private String requireTenant(String tenantId) {
        String normalized = tenantId == null ? "" : tenantId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z][a-z0-9_]{2,79}$")) {
            throw new BusinessException(
                    "MESSAGE_TENANT_REQUIRED",
                    "Selecciona un tenant valido para esta audiencia."
            );
        }
        return normalized;
    }

    private String safeSchema(String schema) {
        String normalized = schema == null ? "" : schema.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z][a-z0-9_]{2,62}$")) {
            throw BusinessException.internal(
                    "MESSAGE_TENANT_SCHEMA_INVALID",
                    "El esquema del tenant no tiene un formato seguro."
            );
        }
        return normalized;
    }

    private String normalizeRole(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private void addAll(Map<String, RecipientTarget> target, Collection<RecipientTarget> values) {
        values.forEach(value -> target.putIfAbsent(value.key(), value));
    }

    public record RecipientTarget(
            MessageRecipientScope scope,
            String tenantId,
            Long userId,
            String username,
            String displayName
    ) {
        String key() {
            return scope + ":" + tenantId + ":" + userId;
        }
    }
}
