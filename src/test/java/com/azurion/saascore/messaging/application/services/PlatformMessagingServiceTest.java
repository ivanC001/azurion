package com.azurion.saascore.messaging.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.messaging.application.dto.SendPlatformMessageRequest;
import com.azurion.saascore.messaging.domain.entities.MessageAudience;
import com.azurion.saascore.messaging.domain.entities.MessagePriority;
import com.azurion.saascore.messaging.domain.entities.MessageRecipientScope;
import com.azurion.saascore.messaging.domain.entities.PlatformMessage;
import com.azurion.saascore.messaging.domain.entities.PlatformMessageRecipient;
import com.azurion.saascore.messaging.domain.repositories.PlatformMessageRecipientRepository;
import com.azurion.saascore.messaging.domain.repositories.PlatformMessageRepository;
import com.azurion.security.jwt.TenantAuthenticationDetails;
import com.azurion.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

class PlatformMessagingServiceTest {

    private final PlatformMessageRepository messageRepository = mock(PlatformMessageRepository.class);
    private final PlatformMessageRecipientRepository recipientRepository =
            mock(PlatformMessageRecipientRepository.class);
    private final MessageRecipientResolver recipientResolver = mock(MessageRecipientResolver.class);
    private final PlatformMessagingService service = new PlatformMessagingService(
            messageRepository,
            recipientRepository,
            recipientResolver
    );

    @Test
    void materializesOneMailboxCopyForEveryResolvedRecipient() {
        Authentication authentication = authentication(7L, "public", "platform.admin");
        when(recipientResolver.resolve(MessageAudience.TENANT_USERS, "tenant_demo", null))
                .thenReturn(List.of(
                        new MessageRecipientResolver.RecipientTarget(
                                MessageRecipientScope.TENANT,
                                "tenant_demo",
                                1L,
                                "admin",
                                "Administrador"
                        ),
                        new MessageRecipientResolver.RecipientTarget(
                                MessageRecipientScope.TENANT,
                                "tenant_demo",
                                2L,
                                "ventas",
                                "Usuario Ventas"
                        )
                ));
        when(messageRepository.save(any(PlatformMessage.class))).thenAnswer(invocation -> {
            PlatformMessage message = invocation.getArgument(0);
            message.setId(11L);
            return message;
        });
        when(recipientRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.send(
                new SendPlatformMessageRequest(
                        "Aviso",
                        "Contenido para el tenant",
                        MessagePriority.WARNING,
                        MessageAudience.TENANT_USERS,
                        "tenant_demo",
                        null,
                        null
                ),
                authentication
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PlatformMessageRecipient>> captor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .extracting(PlatformMessageRecipient::getUserId)
                .containsExactly(1L, 2L);
        assertThat(response.recipientCount()).isEqualTo(2);
        assertThat(response.tenantId()).isEqualTo("tenant_demo");
    }

    @Test
    void refusesPlatformBroadcastFromTenantIdentity() {
        Authentication authentication = authentication(3L, "tenant_demo", "admin");

        assertThatThrownBy(() -> service.send(
                new SendPlatformMessageRequest(
                        "Aviso",
                        "Contenido",
                        MessagePriority.INFO,
                        MessageAudience.ALL_USERS,
                        null,
                        null,
                        null
                ),
                authentication
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("MESSAGE_PLATFORM_ADMIN_REQUIRED");
    }

    @Test
    void cannotMarkAnotherUsersRecipientAsRead() {
        Authentication authentication = authentication(3L, "tenant_demo", "admin");
        when(recipientRepository.findByIdAndRecipientScopeAndTenantIdAndUserId(
                99L,
                MessageRecipientScope.TENANT,
                "tenant_demo",
                3L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(99L, authentication))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("MESSAGE_NOT_FOUND");
    }

    private Authentication authentication(Long userId, String sessionTenant, String username) {
        Authentication authentication = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        TenantAuthenticationDetails details = new TenantAuthenticationDetails(
                request,
                userId,
                sessionTenant,
                sessionTenant,
                "session-1"
        );
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(details);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }
}
