package com.azurion.saascore.ventas.infrastructure.realtime;

import com.azurion.saascore.ventas.application.dto.VentaStatusRealtimeEvent;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class VentaStatusRealtimeStreamService {

    private static final long STREAM_TIMEOUT_MS = 60L * 60L * 1000L;
    private static final String EVENT_NAME_STATUS = "venta-status";
    private static final String EVENT_NAME_CONNECTED = "connected";

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByTenant = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> tenantEmitters = emittersByTenant.computeIfAbsent(normalizedTenant, key -> new CopyOnWriteArrayList<>());
        tenantEmitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(normalizedTenant, emitter));
        emitter.onTimeout(() -> removeEmitter(normalizedTenant, emitter));
        emitter.onError(error -> removeEmitter(normalizedTenant, emitter));

        sendConnectedEvent(emitter, normalizedTenant);
        return emitter;
    }

    public void publish(VentaStatusRealtimeEvent event) {
        if (event == null || event.externalId() == null || event.externalId().isBlank()) {
            return;
        }

        String normalizedTenant = normalizeTenant(event.tenantId());
        List<SseEmitter> emitters = emittersByTenant.get(normalizedTenant);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(EVENT_NAME_STATUS)
                        .data(event));
            } catch (IOException exception) {
                log.debug("No se pudo emitir SSE de venta para tenant {}: {}", normalizedTenant, exception.getMessage());
                removeEmitter(normalizedTenant, emitter);
                emitter.completeWithError(exception);
            } catch (Exception exception) {
                log.debug("SSE venta finalizada para tenant {}: {}", normalizedTenant, exception.getMessage());
                removeEmitter(normalizedTenant, emitter);
                emitter.complete();
            }
        }
    }

    private void sendConnectedEvent(SseEmitter emitter, String tenantId) {
        try {
            emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(EVENT_NAME_CONNECTED)
                    .data(Map.of(
                            "tenantId", tenantId,
                            "connectedAt", OffsetDateTime.now().toString()
                    )));
        } catch (IOException exception) {
            removeEmitter(tenantId, emitter);
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(String tenantId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> tenantEmitters = emittersByTenant.get(tenantId);
        if (tenantEmitters == null) {
            return;
        }
        tenantEmitters.remove(emitter);
        if (tenantEmitters.isEmpty()) {
            emittersByTenant.remove(tenantId, tenantEmitters);
        }
    }

    private String normalizeTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "public";
        }
        return tenantId.trim();
    }
}
