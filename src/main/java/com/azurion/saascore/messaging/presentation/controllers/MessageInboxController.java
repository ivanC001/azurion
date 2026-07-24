package com.azurion.saascore.messaging.presentation.controllers;

import com.azurion.saascore.messaging.application.dto.InboxMessageResponse;
import com.azurion.saascore.messaging.application.dto.MessageUnreadCountResponse;
import com.azurion.saascore.messaging.application.services.PlatformMessagingService;
import com.azurion.shared.api.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/messages/inbox")
@RequiredArgsConstructor
public class MessageInboxController {

    private final PlatformMessagingService service;

    @GetMapping
    public ApiResponse<List<InboxMessageResponse>> inbox(
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(service.inbox(authentication, limit), "Bandeja de mensajes");
    }

    @GetMapping("/unread-count")
    public ApiResponse<MessageUnreadCountResponse> unreadCount(Authentication authentication) {
        return ApiResponse.ok(service.unreadCount(authentication), "Mensajes pendientes");
    }

    @PatchMapping("/{recipientId}/read")
    public ApiResponse<InboxMessageResponse> markRead(
            @PathVariable Long recipientId,
            Authentication authentication
    ) {
        return ApiResponse.ok(service.markRead(recipientId, authentication), "Mensaje marcado como leido");
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllRead(Authentication authentication) {
        return ApiResponse.ok(service.markAllRead(authentication), "Mensajes marcados como leidos");
    }
}
