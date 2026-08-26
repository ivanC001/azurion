package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.events.WhatsappInboundMessageStoredEvent;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplyDispatch;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappMessage;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappAutoReplyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappAutoReplyDispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappAutoReplyEnqueueService {

    private final CrmWhatsappAutoReplyConfigRepository configRepository;
    private final CrmWhatsappAutoReplyDispatchRepository dispatchRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueIfEnabled(CrmWhatsappMessage incomingMessage) {
        boolean enabled = configRepository.findFirstByOrderByIdAsc()
                .map(config -> config.isActivo() && config.getMensaje() != null && !config.getMensaje().isBlank())
                .orElse(false);
        if (!enabled || dispatchRepository.existsByIncomingMessage_Id(incomingMessage.getId())) {
            return;
        }

        CrmWhatsappAutoReplyDispatch dispatch = new CrmWhatsappAutoReplyDispatch();
        dispatch.setIncomingMessage(incomingMessage);
        dispatch.setProspecto(incomingMessage.getProspecto());
        dispatch.setEstado("PENDIENTE");
        CrmWhatsappAutoReplyDispatch saved = dispatchRepository.save(dispatch);
        publishAfterCommit(new WhatsappInboundMessageStoredEvent(TenantContext.getTenantId(), saved.getId()));
    }

    private void publishAfterCommit(WhatsappInboundMessageStoredEvent event) {
        Runnable publish = () -> {
            try {
                eventPublisher.publishEvent(event);
            } catch (RuntimeException exception) {
                log.error("No se pudo programar la respuesta automatica de WhatsApp dispatch={}", event.dispatchId(), exception);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }
}
