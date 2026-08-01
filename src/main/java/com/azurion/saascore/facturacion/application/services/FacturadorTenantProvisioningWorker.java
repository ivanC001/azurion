package com.azurion.saascore.facturacion.application.services;

import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
        name = "azurion.facturador.provisioning-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class FacturadorTenantProvisioningWorker {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(2);
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)
    );

    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final FacturadorClient facturadorClient;
    @Qualifier("facturacionExecutor")
    private final Executor executor;
    private final String workerId = UUID.randomUUID().toString();

    public FacturadorTenantProvisioningWorker(
            EmpresaRepository empresaRepository,
            EmpresaModuloRepository empresaModuloRepository,
            FacturadorClient facturadorClient,
            @Qualifier("facturacionExecutor") Executor executor
    ) {
        this.empresaRepository = empresaRepository;
        this.empresaModuloRepository = empresaModuloRepository;
        this.facturadorClient = facturadorClient;
        this.executor = executor;
    }

    @Scheduled(
            initialDelayString = "${azurion.facturador.provisioning.initial-delay-millis:3000}",
            fixedDelayString = "${azurion.facturador.provisioning.poll-delay-millis:5000}"
    )
    public void poll() {
        OffsetDateTime now = OffsetDateTime.now();
        empresaRepository.recoverExpiredFacturadorLeases(now, now.toLocalDateTime());
        for (Empresa candidate : empresaRepository
                .findTop20ByFacturadorStatusInAndFacturadorNextAttemptAtLessThanEqualOrderByIdAsc(
                        List.of(Empresa.FACTURADOR_STATUS_PENDIENTE, Empresa.FACTURADOR_STATUS_REINTENTO),
                        now
                )) {
            if (empresaRepository.claimFacturadorProvisioning(
                    candidate.getId(),
                    workerId,
                    now,
                    now.plus(LEASE_DURATION),
                    now.toLocalDateTime()
            ) != 1) {
                continue;
            }
            empresaRepository.findByIdAndFacturadorStatusAndFacturadorLeaseOwner(
                    candidate.getId(),
                    Empresa.FACTURADOR_STATUS_PROVISIONANDO,
                    workerId
            ).ifPresent(this::submit);
        }
    }

    private void submit(Empresa empresa) {
        try {
            executor.execute(() -> provision(empresa.getId()));
        } catch (RejectedExecutionException exception) {
            markFailure(empresa.getId(), "Pool de integracion temporalmente saturado");
        }
    }

    private void provision(Long empresaId) {
        try {
            Empresa empresa = requireOwnedLease(empresaId);
            boolean erpActive = empresaModuloRepository.existsActiveModule(empresaId, "ERP", LocalDate.now());
            if (!erpActive || !empresa.isActivo()) {
                facturadorClient.provisionarTenant(
                        empresa.getTenantId(),
                        empresa.getRazonSocial(),
                        empresa.getPaisCodigo(),
                        empresa.getRuc(),
                        false
                );
                markSuspended(requireOwnedLease(empresaId));
                return;
            }

            FacturadorClient.FacturadorTenantProvisioningResult result = facturadorClient.provisionarTenant(
                    empresa.getTenantId(),
                    empresa.getRazonSocial(),
                    empresa.getPaisCodigo(),
                    empresa.getRuc(),
                    empresa.isActivo()
            );

            Empresa current = requireOwnedLease(empresaId);
            current.setFacturadorStatus(Empresa.FACTURADOR_STATUS_PROVISIONADO);
            current.setFacturadorDocumentMode(upperOrDefault(
                    result.documentMode(),
                    Empresa.FACTURADOR_DOCUMENT_MODE_TICKET_ONLY
            ));
            current.setFacturadorFiscalStatus(upperOrDefault(
                    result.fiscalStatus(),
                    Empresa.FACTURADOR_FISCAL_STATUS_NOT_CONFIGURED
            ));
            current.setFacturadorSunatMode(upperOrDefault(
                    result.sunatMode(),
                    Empresa.FACTURADOR_SUNAT_MODE_DISABLED
            ));
            current.setFacturadorLastError(null);
            current.setFacturadorProvisionedAt(OffsetDateTime.now());
            current.setFacturadorNextAttemptAt(null);
            current.setFacturadorLeaseOwner(null);
            current.setFacturadorLeaseUntil(null);
            empresaRepository.save(current);
        } catch (Exception exception) {
            markFailure(empresaId, exception.getMessage());
        }
    }

    private void markSuspended(Empresa empresa) {
        empresa.setFacturadorStatus(Empresa.FACTURADOR_STATUS_SUSPENDIDO);
        empresa.setFacturadorLastError(null);
        empresa.setFacturadorNextAttemptAt(null);
        empresa.setFacturadorLeaseOwner(null);
        empresa.setFacturadorLeaseUntil(null);
        empresaRepository.save(empresa);
    }

    private void markFailure(Long empresaId, String message) {
        empresaRepository.findByIdAndFacturadorStatusAndFacturadorLeaseOwner(
                empresaId,
                Empresa.FACTURADOR_STATUS_PROVISIONANDO,
                workerId
        ).ifPresent(empresa -> {
            int attempts = empresa.getFacturadorAttempts() == null ? 1 : empresa.getFacturadorAttempts();
            boolean exhausted = attempts >= MAX_ATTEMPTS;
            OffsetDateTime now = OffsetDateTime.now();
            empresa.setFacturadorStatus(exhausted
                    ? Empresa.FACTURADOR_STATUS_ERROR
                    : Empresa.FACTURADOR_STATUS_REINTENTO);
            empresa.setFacturadorNextAttemptAt(exhausted
                    ? null
                    : now.plus(RETRY_DELAYS.get(Math.min(attempts - 1, RETRY_DELAYS.size() - 1))));
            empresa.setFacturadorLastError(trimError(message));
            empresa.setFacturadorLeaseOwner(null);
            empresa.setFacturadorLeaseUntil(null);
            empresaRepository.save(empresa);
            log.warn(
                    "Aprovisionamiento de facturador fallo para tenant {} en intento {}. Estado: {}",
                    empresa.getTenantId(),
                    attempts,
                    empresa.getFacturadorStatus()
            );
        });
    }

    private Empresa requireOwnedLease(Long empresaId) {
        return empresaRepository.findByIdAndFacturadorStatusAndFacturadorLeaseOwner(
                empresaId,
                Empresa.FACTURADOR_STATUS_PROVISIONANDO,
                workerId
        ).orElseThrow(() -> new IllegalStateException("La tarea de aprovisionamiento ya no pertenece a este worker"));
    }

    private String upperOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimError(String message) {
        String safe = message == null || message.isBlank() ? "Error no especificado" : message.trim();
        return safe.length() <= 1000 ? safe : safe.substring(0, 1000);
    }
}
