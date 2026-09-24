package com.azurion.saascore.crm.application.events;

/**
 * Aviso de lead listo para enviarse. Viaja con el tenant porque el listener corre
 * fuera del hilo de la peticion y necesita reponer el contexto para leer su SMTP.
 */
public record CrmLeadNotificationQueuedEvent(String tenantId, Long dispatchId) {
}
