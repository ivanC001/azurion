package com.azurion.saascore.crm.presentation.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicLeadReceiptResponse;
import com.azurion.saascore.crm.application.services.CrmLandingIngressRegistryService;
import com.azurion.saascore.crm.application.services.PublicCrmTenantResolver;
import com.azurion.saascore.crm.application.services.PublicLeadSubmissionAuditService;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class PublicCrmFormControllerTest {

    @Test
    void sourceKeyDeLaRutaResuelveTenantYSobrescribeDatosDelCuerpo() {
        CrmLandingIngressRegistryService registry = mock(CrmLandingIngressRegistryService.class);
        PublicCrmTenantResolver tenantResolver = mock(PublicCrmTenantResolver.class);
        CrmUseCaseService crmUseCaseService = mock(CrmUseCaseService.class);
        PublicLeadSubmissionAuditService submissionAuditService = mock(PublicLeadSubmissionAuditService.class);
        PublicCrmFormController controller =
                new PublicCrmFormController(registry, tenantResolver, crmUseCaseService, submissionAuditService);

        String sourceKey = "lnd_publica";
        String tenantId = "empresa_demo";
        when(registry.resolveBrowserSource(sourceKey))
                .thenReturn(new CrmLandingIngressRegistryService.ResolvedIngress(sourceKey, tenantId, 7L));
        PublicLeadReceiptResponse receipt =
                new PublicLeadReceiptResponse("lead_123", "RECEIVED", OffsetDateTime.now());
        when(crmUseCaseService.capturePublicLead(
                org.mockito.ArgumentMatchers.any(PublicCrmLeadRequest.class),
                org.mockito.ArgumentMatchers.eq("BROWSER"),
                org.mockito.ArgumentMatchers.eq("request-123")
        )).thenReturn(receipt);

        var response = controller.submit(sourceKey, "request-123", request("tenant_falso", "lnd_falsa"));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(receipt, response.getBody().data());
        verify(tenantResolver).resolve(tenantId);

        ArgumentCaptor<PublicCrmLeadRequest> requestCaptor =
                ArgumentCaptor.forClass(PublicCrmLeadRequest.class);
        verify(crmUseCaseService).capturePublicLead(
                requestCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("BROWSER"),
                org.mockito.ArgumentMatchers.eq("request-123")
        );
        assertEquals(tenantId, requestCaptor.getValue().rucTenant());
        assertEquals(sourceKey, requestCaptor.getValue().landingKey());
    }

    @Test
    void auditaValidacionRechazadaSinOcultarElErrorAlCliente() {
        CrmLandingIngressRegistryService registry = mock(CrmLandingIngressRegistryService.class);
        PublicCrmTenantResolver tenantResolver = mock(PublicCrmTenantResolver.class);
        CrmUseCaseService crmUseCaseService = mock(CrmUseCaseService.class);
        PublicLeadSubmissionAuditService submissionAuditService = mock(PublicLeadSubmissionAuditService.class);
        PublicCrmFormController controller =
                new PublicCrmFormController(registry, tenantResolver, crmUseCaseService, submissionAuditService);

        when(registry.resolveBrowserSource("lnd_publica"))
                .thenReturn(new CrmLandingIngressRegistryService.ResolvedIngress(
                        "lnd_publica",
                        "empresa_demo",
                        7L
                ));
        when(crmUseCaseService.capturePublicLead(
                org.mockito.ArgumentMatchers.any(PublicCrmLeadRequest.class),
                org.mockito.ArgumentMatchers.eq("BROWSER"),
                org.mockito.ArgumentMatchers.eq("request-invalid")
        )).thenThrow(new BusinessException("CRM_LEAD_CONTACTO_REQUERIDO", "Debe enviar telefono o correo"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.submit(
                        "lnd_publica",
                        "request-invalid",
                        request("tenant_falso", "lnd_falsa")
                )
        );

        assertEquals("CRM_LEAD_CONTACTO_REQUERIDO", exception.getCode());
        verify(submissionAuditService).recordRejected(
                "lnd_publica",
                "BROWSER",
                "request-invalid",
                "CRM_LEAD_CONTACTO_REQUERIDO",
                "Debe enviar telefono o correo"
        );
    }

    private PublicCrmLeadRequest request(String tenantId, String landingKey) {
        return new PublicCrmLeadRequest(
                tenantId,
                landingKey,
                null,
                null,
                null,
                "Juan Perez",
                null,
                "juan@perez.com",
                "999999999",
                null,
                null,
                "LANDING",
                "municipios",
                "https://landing.example/contacto",
                "Deseo informacion",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                null
        );
    }
}
