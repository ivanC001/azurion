package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.VentaFacturacionAsyncTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchVentaFacturacionAsyncUseCase {

    @Qualifier("eventExecutor")
    private final Executor eventExecutor;
    private final ProcessVentaFacturacionAsyncUseCase processVentaFacturacionAsyncUseCase;

    public void dispatch(VentaFacturacionAsyncTask task) {
        CompletableFuture.runAsync(() -> processVentaFacturacionAsyncUseCase.execute(task), eventExecutor)
                .exceptionally(error -> {
                    log.error("No se pudo iniciar procesamiento async de venta {}", task.externalId(), error);
                    return null;
                });
    }
}
