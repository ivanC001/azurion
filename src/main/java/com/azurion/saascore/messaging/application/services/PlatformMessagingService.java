package com.azurion.saascore.messaging.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.messaging.application.dto.InboxMessageResponse;
import com.azurion.saascore.messaging.application.dto.MessageUnreadCountResponse;
import com.azurion.saascore.messaging.application.dto.SendPlatformMessageRequest;
import com.azurion.saascore.messaging.application.dto.SentPlatformMessageResponse;
import com.azurion.saascore.messaging.domain.entities.MessageAudience;
import com.azurion.saascore.messaging.domain.entities.MessageRecipientScope;
import com.azurion.saascore.messaging.domain.entities.PlatformMessage;
import com.azurion.saascore.messaging.domain.entities.PlatformMessageRecipient;
import com.azurion.saascore.messaging.domain.repositories.PlatformMessageRecipientRepository;
import com.azurion.saascore.messaging.domain.repositories.PlatformMessageRepository;
import com.azurion.security.jwt.TenantAuthenticationDetails;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformMessagingService {

    private final PlatformMessageRepository messageRepository;
    private final PlatformMessageRecipientRepository recipientRepository;
    private final MessageRecipientResolver recipientResolver;

    @Transactional
    public SentPlatformMessageResponse send(
            SendPlatformMessageRequest request,
            Authentication authentication
    ) {
        MessageIdentity sender = identity(authentication);
        if (sender.scope() != MessageRecipientScope.PLATFORM) {
            throw BusinessException.forbidden(
                    "MESSAGE_PLATFORM_ADMIN_REQUIRED",
                    "Solo un administrador general puede enviar mensajes de plataforma."
            );
        }
        String tenantId = requiresTenant(request.audiencia()) ? trim(request.tenantId()) : null;
        List<MessageRecipientResolver.RecipientTarget> targets = recipientResolver.resolve(
                request.audiencia(),
                tenantId,
                request.usuarioIds()
        );
        LocalDateTime now = LocalDateTime.now();
        if (request.expiraEn() != null && !request.expiraEn().isAfter(now)) {
            throw new BusinessException(
                    "MESSAGE_EXPIRATION_INVALID",
                    "La fecha de expiracion debe ser posterior al momento del envio."
            );
        }

        PlatformMessage message = new PlatformMessage();
        message.setAsunto(trim(request.asunto()));
        message.setContenido(trim(request.contenido()));
        message.setPrioridad(request.prioridad());
        message.setAudiencia(request.audiencia());
        message.setTenantId(tenantId);
        message.setEnviadoPorUsuarioId(sender.userId());
        message.setEnviadoPorUsername(authentication.getName());
        message.setPublicadoEn(now);
        message.setExpiraEn(request.expiraEn());
        message.setActivo(true);
        PlatformMessage saved = messageRepository.save(message);

        List<PlatformMessageRecipient> recipients = targets.stream().map(target -> {
            PlatformMessageRecipient recipient = new PlatformMessageRecipient();
            recipient.setMessage(saved);
            recipient.setRecipientScope(target.scope());
            recipient.setTenantId(target.tenantId());
            recipient.setUserId(target.userId());
            recipient.setUsernameSnapshot(target.username());
            recipient.setDisplayNameSnapshot(target.displayName());
            return recipient;
        }).toList();
        recipientRepository.saveAll(recipients);
        return toSentResponse(saved, recipients.size(), 0);
    }

    @Transactional(readOnly = true)
    public List<InboxMessageResponse> inbox(Authentication authentication, int limit) {
        MessageIdentity identity = identity(authentication);
        return recipientRepository.findInbox(
                        identity.scope(),
                        identity.tenantId(),
                        identity.userId(),
                        LocalDateTime.now(),
                        PageRequest.of(0, safeLimit(limit))
                ).stream()
                .map(this::toInboxResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MessageUnreadCountResponse unreadCount(Authentication authentication) {
        MessageIdentity identity = identity(authentication);
        return new MessageUnreadCountResponse(recipientRepository.countUnread(
                identity.scope(),
                identity.tenantId(),
                identity.userId(),
                LocalDateTime.now()
        ));
    }

    @Transactional
    public InboxMessageResponse markRead(Long recipientId, Authentication authentication) {
        MessageIdentity identity = identity(authentication);
        PlatformMessageRecipient recipient = recipientRepository
                .findByIdAndRecipientScopeAndTenantIdAndUserId(
                        recipientId,
                        identity.scope(),
                        identity.tenantId(),
                        identity.userId()
                )
                .orElseThrow(() -> BusinessException.notFound(
                        "MESSAGE_NOT_FOUND",
                        "El mensaje no existe en tu bandeja."
                ));
        if (recipient.getReadAt() == null) {
            recipient.setReadAt(LocalDateTime.now());
            recipient = recipientRepository.save(recipient);
        }
        return toInboxResponse(recipient);
    }

    @Transactional
    public int markAllRead(Authentication authentication) {
        MessageIdentity identity = identity(authentication);
        return recipientRepository.markAllRead(
                identity.scope(),
                identity.tenantId(),
                identity.userId(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<SentPlatformMessageResponse> sentMessages(int limit) {
        return messageRepository.findAllByOrderByPublicadoEnDescIdDesc(
                        PageRequest.of(0, safeLimit(limit))
                ).stream()
                .map(message -> toSentResponse(
                        message,
                        recipientRepository.countByMessageId(message.getId()),
                        recipientRepository.countByMessageIdAndReadAtIsNotNull(message.getId())
                ))
                .toList();
    }

    private MessageIdentity identity(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof TenantAuthenticationDetails details)
                || details.getUserId() == null) {
            throw BusinessException.unauthorized(
                    "MESSAGE_IDENTITY_REQUIRED",
                    "No se pudo identificar al usuario de la bandeja."
            );
        }
        String sessionTenant = trim(details.getSessionTenantId());
        if (sessionTenant == null || sessionTenant.isBlank()) {
            sessionTenant = TenantContext.DEFAULT_TENANT;
        }
        boolean platform = TenantContext.DEFAULT_TENANT.equalsIgnoreCase(sessionTenant);
        return new MessageIdentity(
                platform ? MessageRecipientScope.PLATFORM : MessageRecipientScope.TENANT,
                platform ? TenantContext.DEFAULT_TENANT : sessionTenant,
                details.getUserId()
        );
    }

    private boolean requiresTenant(MessageAudience audience) {
        return audience == MessageAudience.TENANT_ADMINS
                || audience == MessageAudience.TENANT_USERS
                || audience == MessageAudience.SELECTED_USERS;
    }

    private InboxMessageResponse toInboxResponse(PlatformMessageRecipient recipient) {
        PlatformMessage message = recipient.getMessage();
        return new InboxMessageResponse(
                recipient.getId(),
                message.getId(),
                message.getAsunto(),
                message.getContenido(),
                message.getPrioridad(),
                message.getAudiencia(),
                message.getTenantId(),
                message.getEnviadoPorUsername(),
                message.getPublicadoEn(),
                message.getExpiraEn(),
                recipient.getReadAt() != null,
                recipient.getReadAt()
        );
    }

    private SentPlatformMessageResponse toSentResponse(
            PlatformMessage message,
            long recipientCount,
            long readCount
    ) {
        return new SentPlatformMessageResponse(
                message.getId(),
                message.getAsunto(),
                message.getContenido(),
                message.getPrioridad(),
                message.getAudiencia(),
                message.getTenantId(),
                message.getEnviadoPorUsername(),
                message.getPublicadoEn(),
                message.getExpiraEn(),
                message.isActivo(),
                recipientCount,
                readCount
        );
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private record MessageIdentity(MessageRecipientScope scope, String tenantId, Long userId) {
    }
}
