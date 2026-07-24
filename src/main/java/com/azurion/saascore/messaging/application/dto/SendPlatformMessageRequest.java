package com.azurion.saascore.messaging.application.dto;

import com.azurion.saascore.messaging.domain.entities.MessageAudience;
import com.azurion.saascore.messaging.domain.entities.MessagePriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record SendPlatformMessageRequest(
        @NotBlank @Size(max = 180) String asunto,
        @NotBlank @Size(max = 5000) String contenido,
        @NotNull MessagePriority prioridad,
        @NotNull MessageAudience audiencia,
        @Pattern(regexp = "^[a-z][a-z0-9_]{2,79}$") String tenantId,
        @Size(max = 1000) List<Long> usuarioIds,
        @Future LocalDateTime expiraEn
) {
}
