package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.SendWhatsappTemplateRequest;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappReengagementOutbox;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappConversationRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WhatsappReengagementWorkerTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Mock
    private CrmWhatsappReengagementOutboxRepository outboxRepository;
    @Mock
    private CrmProspectoRepository prospectoRepository;
    @Mock
    private CrmWhatsappConversationRepository conversationRepository;
    @Mock
    private WhatsappIntegrationService whatsappIntegrationService;
    @Mock
    private WhatsappReengagementService reengagementService;
    @Mock
    private WhatsappAutoReplyConfigurationService autoReplyConfigurationService;

    @InjectMocks
    private WhatsappReengagementWorker worker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(worker, "businessHourStart", 9);
        ReflectionTestUtils.setField(worker, "businessHourEnd", 20);
        ReflectionTestUtils.setField(worker, "businessDays", "1,2,3,4,5,6");
    }

    @Test
    void reconoceElHorarioHabil() {
        // Miercoles 2026-09-02.
        assertTrue(worker.insideBusinessHours(ZonedDateTime.of(2026, 9, 2, 10, 0, 0, 0, LIMA)));
        assertTrue(worker.insideBusinessHours(ZonedDateTime.of(2026, 9, 2, 9, 0, 0, 0, LIMA)));
        assertFalse(worker.insideBusinessHours(ZonedDateTime.of(2026, 9, 2, 20, 0, 0, 0, LIMA)));
        assertFalse(worker.insideBusinessHours(ZonedDateTime.of(2026, 9, 2, 7, 0, 0, 0, LIMA)));
        // Domingo 2026-09-06: fuera de los dias configurados.
        assertFalse(worker.insideBusinessHours(ZonedDateTime.of(2026, 9, 6, 11, 0, 0, 0, LIMA)));
    }

    @Test
    void corrigeElHorarioAlProximoDiaHabil() {
        // Miercoles a las 22:00 -> jueves 09:00.
        assertEquals(
                ZonedDateTime.of(2026, 9, 3, 9, 0, 0, 0, LIMA),
                worker.nextOpening(ZonedDateTime.of(2026, 9, 2, 22, 0, 0, 0, LIMA))
        );
        // Miercoles a las 07:00 -> ese mismo dia a las 09:00.
        assertEquals(
                ZonedDateTime.of(2026, 9, 2, 9, 0, 0, 0, LIMA),
                worker.nextOpening(ZonedDateTime.of(2026, 9, 2, 7, 0, 0, 0, LIMA))
        );
        // Sabado a las 21:00 -> lunes 09:00, porque el domingo no es habil.
        assertEquals(
                ZonedDateTime.of(2026, 9, 7, 9, 0, 0, 0, LIMA),
                worker.nextOpening(ZonedDateTime.of(2026, 9, 5, 21, 0, 0, 0, LIMA))
        );
    }

    @Test
    void omiteElEnvioCuandoElClienteYaRespondio() {
        prepararTarea();
        CrmProspecto prospecto = prospecto(11L);
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto));

        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setUltimoEntranteEn(OffsetDateTime.now(ZoneOffset.UTC).minusHours(2));
        when(conversationRepository.findByProspecto_Id(11L)).thenReturn(Optional.of(conversation));

        worker.poll();

        verify(whatsappIntegrationService, never()).sendTemplate(any(), any());
        ArgumentCaptor<String> resultado = ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).markResolved(eq(1L), anyString(), eq("SKIPPED"),
                resultado.capture(), any(LocalDateTime.class));
        assertTrue(resultado.getValue().contains("ventana"));
    }

    @Test
    void omiteElEnvioCuandoElProspectoSeDioDeBaja() {
        prepararTarea();
        CrmProspecto prospecto = prospecto(11L);
        prospecto.setWhatsappOptoutEn(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto));

        worker.poll();

        verify(whatsappIntegrationService, never()).sendTemplate(any(), any());
        verify(outboxRepository).markResolved(eq(1L), anyString(), eq("SKIPPED"),
                anyString(), any(LocalDateTime.class));
    }

    @Test
    void enviaLaPlantillaCuandoLaVentanaEstaCerrada() {
        prepararTarea();
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto(11L)));

        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setUltimoEntranteEn(OffsetDateTime.now(ZoneOffset.UTC).minusDays(8));
        when(conversationRepository.findByProspecto_Id(11L)).thenReturn(Optional.of(conversation));
        when(reengagementService.readParameters(anyString())).thenReturn(List.of("Ana"));
        when(autoReplyConfigurationService.tenantZone()).thenReturn(zonaSiempreHabil());

        worker.poll();

        ArgumentCaptor<SendWhatsappTemplateRequest> request =
                ArgumentCaptor.forClass(SendWhatsappTemplateRequest.class);
        verify(whatsappIntegrationService).sendTemplate(eq(11L), request.capture());
        assertEquals("seguimiento", request.getValue().nombre());
        assertEquals(List.of("Ana"), request.getValue().parametros());
        verify(outboxRepository).markResolved(eq(1L), anyString(), eq("SENT"),
                anyString(), any(LocalDateTime.class));
    }

    @Test
    void noReintentaCuandoMetaRechazaLaPlantillaDeFormaDefinitiva() {
        prepararTarea();
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto(11L)));
        when(conversationRepository.findByProspecto_Id(11L)).thenReturn(Optional.empty());
        when(reengagementService.readParameters(anyString())).thenReturn(List.of());
        when(autoReplyConfigurationService.tenantZone()).thenReturn(zonaSiempreHabil());
        when(whatsappIntegrationService.sendTemplate(eq(11L), any()))
                .thenThrow(new BusinessException("CRM_WHATSAPP_PLANTILLA_RECHAZADA", "rechazada"));

        worker.poll();

        verify(outboxRepository).markFailedAttempt(eq(1L), anyString(), eq("FAILED"),
                any(LocalDateTime.class), anyString(), any(LocalDateTime.class));
    }

    @Test
    void reintentaCuandoElFalloEsTransitorio() {
        prepararTarea();
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto(11L)));
        when(conversationRepository.findByProspecto_Id(11L)).thenReturn(Optional.empty());
        when(reengagementService.readParameters(anyString())).thenReturn(List.of());
        when(autoReplyConfigurationService.tenantZone()).thenReturn(zonaSiempreHabil());
        when(whatsappIntegrationService.sendTemplate(eq(11L), any()))
                .thenThrow(new BusinessException("CRM_WHATSAPP_NO_DISPONIBLE", "sin conexion"));

        worker.poll();

        verify(outboxRepository).markFailedAttempt(eq(1L), anyString(), eq("RETRY"),
                any(LocalDateTime.class), anyString(), any(LocalDateTime.class));
    }

    /** Encola una tarea ya reclamada por este worker, lista para procesarse. */
    private void prepararTarea() {
        CrmWhatsappReengagementOutbox job = new CrmWhatsappReengagementOutbox();
        job.setId(1L);
        job.setTenantId("azurion_dev");
        job.setProspectoId(11L);
        job.setPlantillaNombre("seguimiento");
        job.setPlantillaIdioma("es_PE");
        job.setParametrosJson("[\"Ana\"]");
        job.setAttempts(1);
        job.setStatus("PROCESSING");

        when(outboxRepository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                any(), any(LocalDateTime.class))).thenReturn(List.of(job));
        when(outboxRepository.claim(eq(1L), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(outboxRepository.findByIdAndStatusAndLeaseOwner(eq(1L), eq("PROCESSING"), anyString()))
                .thenReturn(Optional.of(job));
        org.mockito.Mockito.lenient().when(outboxRepository.markResolved(
                any(), anyString(), anyString(), any(), any(LocalDateTime.class))).thenReturn(1);
    }

    private CrmProspecto prospecto(Long id) {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(id);
        return prospecto;
    }

    /**
     * Zona en la que "ahora" siempre cae dentro del horario configurado, para que la
     * prueba no dependa de la hora a la que se ejecute.
     */
    private ZoneId zonaSiempreHabil() {
        ReflectionTestUtils.setField(worker, "businessHourStart", 0);
        ReflectionTestUtils.setField(worker, "businessHourEnd", 24);
        ReflectionTestUtils.setField(worker, "businessDays", "1,2,3,4,5,6,7");
        return ZoneId.systemDefault();
    }
}
