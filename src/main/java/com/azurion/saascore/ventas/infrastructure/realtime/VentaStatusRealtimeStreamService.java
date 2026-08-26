package com.azurion.saascore.ventas.infrastructure.realtime;

import com.azurion.saascore.ventas.application.dto.VentaStatusRealtimeEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class VentaStatusRealtimeStreamService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VentaStatusRealtimeStreamService.class);
    private static final long STREAM_TIMEOUT_MS = 60L * 60L * 1000L;
    private static final String EVENT_NAME_STATUS = "venta-status";
    private static final String EVENT_NAME_CONNECTED = "connected";

    private final Map<String, CopyOnWriteArrayList<SseConnection>> emittersByTenant = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        SseEmitter emitter = createEmitter();
        SseConnection connection = new SseConnection(emitter);
        CopyOnWriteArrayList<SseConnection> tenantEmitters = emittersByTenant.computeIfAbsent(
                normalizedTenant,
                key -> new CopyOnWriteArrayList<>()
        );
        tenantEmitters.add(connection);

        emitter.onCompletion(() -> removeConnection(normalizedTenant, connection));
        emitter.onTimeout(() -> removeConnection(normalizedTenant, connection));
        emitter.onError(error -> removeConnection(normalizedTenant, connection));

        sendConnectedEvent(connection, normalizedTenant);
        return emitter;
    }

    public void publish(VentaStatusRealtimeEvent event) {
        if (event == null || event.externalId() == null || event.externalId().isBlank()) {
            return;
        }

        String normalizedTenant = normalizeTenant(event.tenantId());
        List<SseConnection> emitters = emittersByTenant.get(normalizedTenant);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseConnection connection : emitters) {
            if (connection.isClosed()) {
                continue;
            }
            try {
                connection.emitter().send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name(EVENT_NAME_STATUS)
                        .data(event));
            } catch (Exception exception) {
                handleSendFailure(normalizedTenant, connection, exception);
            }
        }
    }

    SseEmitter createEmitter() {
        return new SseEmitter(STREAM_TIMEOUT_MS);
    }

    private void sendConnectedEvent(SseConnection connection, String tenantId) {
        try {
            connection.emitter().send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name(EVENT_NAME_CONNECTED)
                    .data(Map.of(
                            "tenantId", tenantId,
                            "connectedAt", OffsetDateTime.now().toString()
                    )));
        } catch (Exception exception) {
            handleSendFailure(tenantId, connection, exception);
        }
    }

    private void handleSendFailure(String tenantId, SseConnection connection, Exception exception) {
        log.debug("Conexion SSE de venta cerrada para tenant {}: {}", tenantId, exception.getMessage());
        removeConnection(tenantId, connection);
        /*
         * SseEmitter.send() ya delega el error al contenedor Servlet. Completar el
         * emitter otra vez desde este hilo reutilizaria un AsyncContext invalidado.
         */
    }

    private void removeConnection(String tenantId, SseConnection connection) {
        connection.close();
        CopyOnWriteArrayList<SseConnection> tenantEmitters = emittersByTenant.get(tenantId);
        if (tenantEmitters == null) {
            return;
        }
        tenantEmitters.remove(connection);
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

    private static final class SseConnection {

        private final SseEmitter emitter;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private SseConnection(SseEmitter emitter) {
            this.emitter = emitter;
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private boolean isClosed() {
            return closed.get();
        }

        private void close() {
            closed.compareAndSet(false, true);
        }
    }
}
