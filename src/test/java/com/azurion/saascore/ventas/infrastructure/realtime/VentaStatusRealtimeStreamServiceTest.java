package com.azurion.saascore.ventas.infrastructure.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.azurion.saascore.ventas.application.dto.VentaStatusRealtimeEvent;
import java.io.IOException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class VentaStatusRealtimeStreamServiceTest {

    @Test
    void removesFailedEmitterWithoutCompletingErroredAsyncContextAgain() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doNothing()
                .doThrow(new IOException("cliente desconectado"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));
        VentaStatusRealtimeStreamService service = serviceUsing(emitter);

        service.subscribe("tenant_demo");
        service.publish(event("tenant_demo", "venta-1"));
        service.publish(event("tenant_demo", "venta-1"));

        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }

    @Test
    void removesEmitterWhenInitialConnectionEventCannotBeSent() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IllegalStateException("AsyncContext finalizado"))
                .when(emitter)
                .send(any(SseEmitter.SseEventBuilder.class));
        VentaStatusRealtimeStreamService service = serviceUsing(emitter);

        service.subscribe("tenant_demo");
        service.publish(event("tenant_demo", "venta-2"));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }

    private VentaStatusRealtimeStreamService serviceUsing(SseEmitter emitter) {
        return new VentaStatusRealtimeStreamService() {
            @Override
            SseEmitter createEmitter() {
                return emitter;
            }
        };
    }

    private VentaStatusRealtimeEvent event(String tenantId, String externalId) {
        return new VentaStatusRealtimeEvent(
                tenantId,
                "test",
                1L,
                externalId,
                "PROCESANDO",
                1,
                null,
                null,
                "03",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now()
        );
    }
}
