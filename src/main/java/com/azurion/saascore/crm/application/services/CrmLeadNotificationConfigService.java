package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.CrmLeadNotificationConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmLeadNotificationDispatchResponse;
import com.azurion.saascore.crm.application.dto.CrmLeadNotificationTestResponse;
import com.azurion.saascore.crm.application.dto.UpdateCrmLeadNotificationConfigRequest;
import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationConfig;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationDispatchRepository;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.settings.email.application.services.EmailSenderService;
import com.azurion.saascore.settings.email.application.services.TenantEmailConfigService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuracion de los avisos por correo del CRM y consulta de su historial.
 */
@Service
@RequiredArgsConstructor
public class CrmLeadNotificationConfigService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final int MAX_COPIAS = 10;
    private static final int MAX_HISTORIAL = 50;

    private final CrmLeadNotificationConfigRepository configRepository;
    private final CrmLeadNotificationDispatchRepository dispatchRepository;
    private final TenantEmailConfigService emailConfigService;
    private final EmailSenderService emailSenderService;
    private final AuthorizationService authorizationService;
    private final UsuarioTenantRepository usuarioTenantRepository;

    @Transactional(readOnly = true)
    public CrmLeadNotificationConfigResponse getConfiguration() {
        return toResponse(findOrCreate());
    }

    @Transactional
    public CrmLeadNotificationConfigResponse updateConfiguration(UpdateCrmLeadNotificationConfigRequest request) {
        CrmLeadNotificationConfig config = findOrCreate();
        if (request.activo() != null) {
            config.setActivo(request.activo());
        }
        if (request.notificarLeadNuevo() != null) {
            config.setNotificarLeadNuevo(request.notificarLeadNuevo());
        }
        if (request.notificarMensajeNuevo() != null) {
            config.setNotificarMensajeNuevo(request.notificarMensajeNuevo());
        }
        if (request.notificarResponsable() != null) {
            config.setNotificarResponsable(request.notificarResponsable());
        }
        if (request.correosCopia() != null) {
            config.setCorreosCopia(String.join(",", normalizeEmails(request.correosCopia())));
        }
        if (request.cooldownMinutos() != null) {
            config.setCooldownMinutos(request.cooldownMinutos());
        }
        // Avisar sin destinatarios seria una configuracion muerta: o va al asesor
        // asignado, o hay al menos un correo de copia.
        if (config.isActivo() && !config.isNotificarResponsable() && parseEmails(config.getCorreosCopia()).isEmpty()) {
            throw new BusinessException(
                    "CRM_NOTIFICACION_SIN_DESTINATARIOS",
                    "Activa el aviso al asesor asignado o agrega al menos un correo de copia");
        }
        return toResponse(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<CrmLeadNotificationDispatchResponse> history() {
        return dispatchRepository.findRecent(PageRequest.of(0, MAX_HISTORIAL)).stream()
                .map(dispatch -> new CrmLeadNotificationDispatchResponse(
                        dispatch.getId(),
                        dispatch.getProspecto().getId(),
                        dispatch.getProspecto().getNombre(),
                        dispatch.getTipo(),
                        parseEmails(dispatch.getDestinatarios()),
                        dispatch.getAsunto(),
                        dispatch.getEstado(),
                        dispatch.getDetalle(),
                        dispatch.getCreatedAt(),
                        dispatch.getProcessedAt()
                ))
                .toList();
    }

    private CrmLeadNotificationConfig findOrCreate() {
        return configRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> configRepository.save(new CrmLeadNotificationConfig()));
    }

    private List<String> normalizeEmails(List<String> values) {
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (normalized.size() > MAX_COPIAS) {
            throw new BusinessException(
                    "CRM_NOTIFICACION_COPIAS_EXCEDIDAS",
                    "Puedes registrar hasta " + MAX_COPIAS + " correos de copia");
        }
        normalized.stream()
                .filter(email -> !EMAIL.matcher(email).matches())
                .findFirst()
                .ifPresent(email -> {
                    throw new BusinessException("CRM_NOTIFICACION_CORREO_INVALIDO", "El correo " + email + " no es valido");
                });
        return normalized;
    }

    private List<String> parseEmails(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }


    /**
     * Envia un aviso de ejemplo con la configuracion actual.
     *
     * Es la forma de comprobar el circuito completo antes de que entre un lead real:
     * valida el SMTP del tenant, los destinatarios resueltos y como se ve el correo.
     * Si el SMTP nunca se habia probado, un envio exitoso lo deja verificado.
     */
    public CrmLeadNotificationTestResponse sendTest() {
        CrmLeadNotificationConfig config = getOrCreateForTest();
        List<String> destinatarios = resolveTestRecipients(config);
        if (destinatarios.isEmpty()) {
            return new CrmLeadNotificationTestResponse(
                    false,
                    List.of(),
                    "No hay a quien avisar: activa el aviso al asesor asignado (y ten un correo en tu perfil) o agrega un correo de copia.");
        }
        try {
            for (String destinatario : destinatarios) {
                emailSenderService.sendEmail(
                        TenantContext.getTenantId(),
                        destinatario,
                        "Prueba de avisos CRM",
                        testBody(),
                        List.of());
            }
            return new CrmLeadNotificationTestResponse(
                    true,
                    destinatarios,
                    "Aviso de prueba enviado. Revisa la bandeja de entrada.");
        } catch (RuntimeException exception) {
            return new CrmLeadNotificationTestResponse(
                    false,
                    destinatarios,
                    firstNonBlank(exception.getMessage(), "No se pudo enviar el aviso de prueba."));
        }
    }

    private CrmLeadNotificationConfig getOrCreateForTest() {
        return configRepository.findFirstByOrderByIdAsc().orElseGet(CrmLeadNotificationConfig::new);
    }

    /** Mismos destinatarios que un aviso real, con el usuario actual como asesor. */
    private List<String> resolveTestRecipients(CrmLeadNotificationConfig config) {
        Set<String> destinatarios = new LinkedHashSet<>();
        if (config.isNotificarResponsable()) {
            Long usuarioId = authorizationService.currentUsuarioId();
            if (usuarioId != null) {
                usuarioTenantRepository.findById(usuarioId)
                        .map(UsuarioTenant::getEmail)
                        .filter(email -> email != null && !email.isBlank())
                        .map(email -> email.trim().toLowerCase(Locale.ROOT))
                        .ifPresent(destinatarios::add);
            }
        }
        destinatarios.addAll(parseEmails(config.getCorreosCopia()));
        return List.copyOf(destinatarios);
    }

    private String testBody() {
        return """
                Este es un aviso de prueba del CRM de Azurion.

                Contacto: Cliente de ejemplo
                Telefono: 900 000 000
                Correo: cliente@ejemplo.test
                Canal: WHATSAPP
                Interes: Producto de ejemplo

                Si recibiste este correo, los avisos de leads nuevos llegaran igual a tu bandeja.
                """;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
    private CrmLeadNotificationConfigResponse toResponse(CrmLeadNotificationConfig config) {
        boolean emailReady = emailConfigService.isCurrentTenantEmailActive();
        return new CrmLeadNotificationConfigResponse(
                config.isActivo(),
                config.isNotificarLeadNuevo(),
                config.isNotificarMensajeNuevo(),
                config.isNotificarResponsable(),
                parseEmails(config.getCorreosCopia()),
                config.getCooldownMinutos(),
                emailReady,
                emailReady
                        ? "Los avisos salen desde el correo configurado del tenant " + TenantContext.getTenantId()
                        : "Configura y verifica el correo del tenant para que salgan los avisos"
        );
    }
}
