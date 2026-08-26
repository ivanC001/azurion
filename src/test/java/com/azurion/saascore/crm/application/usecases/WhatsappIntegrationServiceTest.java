package com.azurion.saascore.crm.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.SendWhatsappMessageRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappQuoteRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappTemplateRequest;
import com.azurion.saascore.crm.application.dto.WhatsappWebhookResult;
import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.application.services.CrmLeadAssignmentService;
import com.azurion.saascore.crm.application.services.CrmPhoneNormalizationService;
import com.azurion.saascore.crm.application.services.WhatsappIntegrationService;
import com.azurion.saascore.crm.application.services.WhatsappAutoReplyEnqueueService;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversationNote;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappMessage;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappConversationRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappConversationNoteRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappMessageRepository;
import com.azurion.saascore.cotizaciones.application.usecases.GenerateCotizacionPdfUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.UpdateCotizacionEstadoUseCase;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient.SendResult;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient.TemplateInfo;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class WhatsappIntegrationServiceTest {

    private static final String APP_SECRET = "meta-app-secret";

    @Mock
    private CrmCanalTokenConfigRepository configRepository;
    @Mock
    private CrmProspectoRepository prospectoRepository;
    @Mock
    private CrmActividadRepository actividadRepository;
    @Mock
    private CrmWhatsappConversationRepository conversationRepository;
    @Mock
    private CrmWhatsappConversationNoteRepository conversationNoteRepository;
    @Mock
    private CrmWhatsappMessageRepository messageRepository;
    @Mock
    private CotizacionRepository cotizacionRepository;
    @Mock
    private GenerateCotizacionPdfUseCase generateCotizacionPdfUseCase;
    @Mock
    private UpdateCotizacionEstadoUseCase updateCotizacionEstadoUseCase;
    @Mock
    private CrmSecretEncryptionService secretEncryptionService;
    @Mock
    private CrmPhoneNormalizationService phoneNormalizationService;
    @Mock
    private WhatsappCloudApiClient cloudApiClient;
    @Mock
    private CrmLeadAssignmentService leadAssignmentService;
    @Mock
    private WhatsappAutoReplyEnqueueService autoReplyEnqueueService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private UsuarioTenantRepository usuarioTenantRepository;

    private WhatsappIntegrationService service;
    private CrmCanalTokenConfig config;

    @BeforeEach
    void setUp() {
        service = new WhatsappIntegrationService(
                configRepository,
                prospectoRepository,
                actividadRepository,
                conversationRepository,
                conversationNoteRepository,
                messageRepository,
                cotizacionRepository,
                generateCotizacionPdfUseCase,
                updateCotizacionEstadoUseCase,
                secretEncryptionService,
                phoneNormalizationService,
                cloudApiClient,
                new ObjectMapper(),
                leadAssignmentService,
                autoReplyEnqueueService,
                transactionTemplate
        );
        org.mockito.Mockito.lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        config = new CrmCanalTokenConfig();
        config.setCanal("WHATSAPP");
        config.setActivo(true);
        config.setPhoneNumberId("1234567890");
        config.setAppSecret("encrypted-app-secret");
        config.setVerifyToken("encrypted-verify-token");
        org.mockito.Mockito.lenient().when(phoneNormalizationService.normalize(any(), any())).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0);
            String digits = raw == null ? null : raw.replaceAll("[^0-9]", "");
            return new CrmPhoneNormalizationService.NormalizedPhone(
                    digits == null || digits.isBlank() ? null : digits,
                    digits == null || digits.isBlank() ? List.of() : List.of(digits)
            );
        });
    }

    @Test
    void createsWhatsappLeadFromSignedWebhook() throws Exception {
        String payload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "field": "messages",
                      "value": {
                        "metadata": {"phone_number_id": "1234567890"},
                        "contacts": [{"wa_id": "51999888777", "profile": {"name": "Ana Perez"}}],
                        "messages": [{
                          "from": "51999888777",
                          "id": "wamid.inbound-1",
                          "timestamp": "1760000000",
                          "type": "text",
                          "text": {"body": "Quiero informacion del producto"}
                        }]
                      }
                    }]
                  }]
                }
                """;
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(secretEncryptionService.decrypt("encrypted-app-secret")).thenReturn(APP_SECRET);
        when(messageRepository.existsByMetaMessageId("wamid.inbound-1")).thenReturn(false);
        when(prospectoRepository.findFirstByTelefonoNormalizado("51999888777")).thenReturn(Optional.empty());
        when(prospectoRepository.save(any(CrmProspecto.class))).thenAnswer(invocation -> {
            CrmProspecto prospecto = invocation.getArgument(0);
            prospecto.setId(10L);
            return prospecto;
        });
        when(messageRepository.save(any(CrmWhatsappMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WhatsappWebhookResult result = service.processWebhook(payload, signature(payload));

        assertEquals(1, result.mensajesProcesados());
        assertEquals(0, result.mensajesDuplicados());
        assertNotNull(config.getLastWebhookAt());
        assertNotNull(config.getLastInboundMessageAt());
        ArgumentCaptor<CrmProspecto> prospectCaptor = ArgumentCaptor.forClass(CrmProspecto.class);
        verify(prospectoRepository).save(prospectCaptor.capture());
        assertEquals("Ana Perez", prospectCaptor.getValue().getNombre());
        assertEquals("SIN_DEFINIR", prospectCaptor.getValue().getTipoPersona());
        assertEquals("WHATSAPP", prospectCaptor.getValue().getOrigen());
        assertEquals("WHATSAPP", prospectCaptor.getValue().getCanalIngreso());
        assertEquals("51999888777", prospectCaptor.getValue().getTelefono());
        verify(actividadRepository).save(any());
        ArgumentCaptor<CrmWhatsappConversation> conversationCaptor =
                ArgumentCaptor.forClass(CrmWhatsappConversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        assertEquals(1, conversationCaptor.getValue().getNoLeidos());
        assertEquals("Quiero informacion del producto", conversationCaptor.getValue().getUltimoMensaje());
    }

    @Test
    void ignoresRetriedMessageByWamid() throws Exception {
        String payload = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages","value":{
                  "metadata":{"phone_number_id":"1234567890"},
                  "messages":[{"from":"51999888777","id":"wamid.duplicate","timestamp":"1760000000","type":"text","text":{"body":"Hola"}}]
                }}]}]}
                """;
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(secretEncryptionService.decrypt("encrypted-app-secret")).thenReturn(APP_SECRET);
        when(messageRepository.existsByMetaMessageId("wamid.duplicate")).thenReturn(true);

        WhatsappWebhookResult result = service.processWebhook(payload, signature(payload));

        assertEquals(0, result.mensajesProcesados());
        assertEquals(1, result.mensajesDuplicados());
        verify(prospectoRepository, never()).save(any());
        verify(actividadRepository, never()).save(any());
    }

    @Test
    void sendsTextToProspectPhoneThroughCloudApi() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setNombre("Luis");
        prospecto.setTelefono("+51 999 888 777");
        prospecto.setEstado("NUEVO");
        prospecto.setNivelInteres("FRIO");
        when(prospectoRepository.findById(44L)).thenReturn(Optional.of(prospecto));
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(cloudApiClient.sendText(eq(config), eq("51999888777"), eq("Hola Luis"), eq(false)))
                .thenReturn(new SendResult("wamid.outbound-1", "51999888777", "{\"messages\":[{\"id\":\"wamid.outbound-1\"}]}"));
        when(messageRepository.save(any(CrmWhatsappMessage.class))).thenAnswer(invocation -> {
            CrmWhatsappMessage message = invocation.getArgument(0);
            message.setId(99L);
            return message;
        });
        mockOpenCustomerServiceWindow(prospecto);

        var response = service.sendMessage(44L, new SendWhatsappMessageRequest("Hola Luis", false));

        assertEquals("wamid.outbound-1", response.metaMessageId());
        assertEquals("SALIENTE", response.direccion());
        assertNotNull(response.mensajeEn());
        verify(actividadRepository).save(any());
        ArgumentCaptor<CrmWhatsappConversation> conversationCaptor =
                ArgumentCaptor.forClass(CrmWhatsappConversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        assertEquals(0, conversationCaptor.getValue().getNoLeidos());
        assertEquals("Hola Luis", conversationCaptor.getValue().getUltimoMensaje());
    }

    @Test
    void marksInboundMessagesAsReadLocallyAndInMeta() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setNombre("Luis");
        prospecto.setTelefono("51999888777");

        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setId(7L);
        conversation.setProspecto(prospecto);
        conversation.setEstado("ABIERTA");
        conversation.setNoLeidos(2);

        CrmWhatsappMessage message = new CrmWhatsappMessage();
        message.setId(8L);
        message.setProspecto(prospecto);
        message.setMetaMessageId("wamid.inbound-read");
        message.setDireccion("ENTRANTE");
        message.setTipoMensaje("text");
        message.setContenido("Hola");
        message.setEstado("RECIBIDO");

        when(conversationRepository.findByProspecto_Id(44L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByProspecto_IdAndDireccionAndLeidoEnIsNull(44L, "ENTRANTE"))
                .thenReturn(List.of(message));
        when(messageRepository.findFirstByProspecto_IdAndDireccionOrderByMensajeEnDescIdDesc(44L, "ENTRANTE"))
                .thenReturn(Optional.of(message));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));

        var response = service.markConversationRead(44L);

        assertEquals(0, response.noLeidos());
        assertNotNull(message.getLeidoEn());
        verify(messageRepository).saveAll(List.of(message));
        verify(cloudApiClient).markAsRead(config, "wamid.inbound-read");
    }

    @Test
    void rejectsFourthInternalNote() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setId(7L);
        conversation.setProspecto(prospecto);
        when(conversationRepository.findForUpdateByProspectoId(44L)).thenReturn(Optional.of(conversation));
        when(conversationNoteRepository.findAllByConversation_IdOrderBySlotAsc(7L))
                .thenReturn(List.of(note(conversation, 1), note(conversation, 2), note(conversation, 3)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createConversationNote(44L, "Una nota adicional")
        );

        assertEquals("CRM_WHATSAPP_NOTAS_LIMITE", exception.getCode());
        verify(conversationNoteRepository, never()).save(any());
    }

    @Test
    void sendsQuoteAsWhatsappDocumentAndMarksItSent() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setNombre("Luis");
        prospecto.setTelefono("+51 999 888 777");
        prospecto.setEstado("NUEVO");
        prospecto.setNivelInteres("FRIO");
        Cotizacion quote = new Cotizacion();
        quote.setId(12L);
        quote.setCrmOportunidadId(90L);
        byte[] pdfBytes = "%PDF-test".getBytes(StandardCharsets.UTF_8);
        when(prospectoRepository.findById(44L)).thenReturn(Optional.of(prospecto));
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(cotizacionRepository.findByIdAndCrmProspectoId(12L, 44L)).thenReturn(Optional.of(quote));
        when(cotizacionRepository.claimWhatsappSend(eq(12L), any(), any(), any())).thenReturn(1);
        when(cotizacionRepository.markWhatsappSent(eq(12L), any(), eq("wamid.quote-12"), any())).thenReturn(1);
        when(generateCotizacionPdfUseCase.execute(12L)).thenReturn(new CotizacionPdfResponse(
                "cotizacion-12.pdf",
                "application/pdf",
                Base64.getEncoder().encodeToString(pdfBytes)
        ));
        when(cloudApiClient.uploadMedia(eq(config), any(byte[].class), eq("cotizacion-12.pdf"), eq("application/pdf")))
                .thenReturn("media-12");
        when(cloudApiClient.sendDocument(
                eq(config),
                eq("51999888777"),
                eq("media-12"),
                eq("cotizacion-12.pdf"),
                eq("Adjunto")
        )).thenReturn(new SendResult("wamid.quote-12", "51999888777", "{\"messages\":[{\"id\":\"wamid.quote-12\"}]}"));
        when(messageRepository.save(any(CrmWhatsappMessage.class))).thenAnswer(invocation -> {
            CrmWhatsappMessage message = invocation.getArgument(0);
            message.setId(55L);
            return message;
        });
        mockOpenCustomerServiceWindow(prospecto);

        var response = service.sendQuote(44L, 12L, new SendWhatsappQuoteRequest("Adjunto"));

        assertEquals("document", response.mensaje().tipoMensaje());
        assertEquals("wamid.quote-12", response.mensaje().metaMessageId());
        verify(updateCotizacionEstadoUseCase).execute(
                12L,
                new UpdateCotizacionEstadoRequest("ENVIADA", "WHATSAPP", null, null, null)
        );
        verify(actividadRepository).save(any());
    }

    @Test
    void rejectsDuplicateWhatsappQuoteSendBeforeCallingMeta() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setNombre("Luis");
        prospecto.setTelefono("+51 999 888 777");
        Cotizacion quote = new Cotizacion();
        quote.setId(12L);
        quote.setWhatsappSendStatus("SENT");
        when(prospectoRepository.findById(44L)).thenReturn(Optional.of(prospecto));
        when(cotizacionRepository.findByIdAndCrmProspectoId(12L, 44L)).thenReturn(Optional.of(quote));
        when(cotizacionRepository.claimWhatsappSend(eq(12L), any(), any(), any())).thenReturn(0);
        when(cotizacionRepository.findById(12L)).thenReturn(Optional.of(quote));
        mockOpenCustomerServiceWindow(prospecto);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendQuote(44L, 12L, new SendWhatsappQuoteRequest("Adjunto"))
        );

        assertEquals("CRM_WHATSAPP_COTIZACION_YA_ENVIADA", exception.getCode());
        verify(cloudApiClient, never()).uploadMedia(any(), any(), any(), any());
        verify(cloudApiClient, never()).sendDocument(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsFreeFormMessageOutsideCustomerServiceWindow() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setTelefono("51999888777");
        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setProspecto(prospecto);
        conversation.setUltimoEntranteEn(OffsetDateTime.now().minusHours(25));
        when(prospectoRepository.findById(44L)).thenReturn(Optional.of(prospecto));
        when(conversationRepository.findByProspecto_Id(44L)).thenReturn(Optional.of(conversation));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendMessage(44L, new SendWhatsappMessageRequest("Hola", false))
        );

        assertEquals("CRM_WHATSAPP_VENTANA_ATENCION_CERRADA", exception.getCode());
        verify(cloudApiClient, never()).sendText(any(), any(), any(), anyBoolean());
    }

    @Test
    void sendsApprovedTemplateOutsideCustomerServiceWindow() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setNombre("Luis");
        prospecto.setTelefono("51999888777");
        prospecto.setEstado("NUEVO");
        prospecto.setNivelInteres("FRIO");
        TemplateInfo template = new TemplateInfo(
                "retomar_contacto",
                "es_PE",
                "UTILITY",
                "Hola {{1}}, retomamos tu consulta sobre {{2}}.",
                2
        );
        when(prospectoRepository.findById(44L)).thenReturn(Optional.of(prospecto));
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(cloudApiClient.listApprovedTemplates(config)).thenReturn(List.of(template));
        when(cloudApiClient.sendTemplate(
                config,
                "51999888777",
                "retomar_contacto",
                "es_PE",
                List.of("Luis", "Python")
        )).thenReturn(new SendResult(
                "wamid.template-1",
                "51999888777",
                "{\"messages\":[{\"id\":\"wamid.template-1\"}]}"
        ));
        when(messageRepository.save(any(CrmWhatsappMessage.class))).thenAnswer(invocation -> {
            CrmWhatsappMessage message = invocation.getArgument(0);
            message.setId(101L);
            return message;
        });

        var response = service.sendTemplate(
                44L,
                new SendWhatsappTemplateRequest(
                        "retomar_contacto",
                        "es_PE",
                        List.of("Luis", "Python")
                )
        );

        assertEquals("template", response.tipoMensaje());
        assertEquals("Hola Luis, retomamos tu consulta sobre Python.", response.contenido());
        assertEquals("wamid.template-1", response.metaMessageId());
        verify(actividadRepository).save(any());
    }

    @Test
    void rejectsTemplateWithMissingParametersBeforeSending() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(44L);
        prospecto.setTelefono("51999888777");
        TemplateInfo template = new TemplateInfo(
                "retomar_contacto",
                "es_PE",
                "UTILITY",
                "Hola {{1}}, tu solicitud {{2}} sigue disponible.",
                2
        );
        when(prospectoRepository.findById(44L)).thenReturn(Optional.of(prospecto));
        when(configRepository.findByCanal("WHATSAPP")).thenReturn(Optional.of(config));
        when(cloudApiClient.listApprovedTemplates(config)).thenReturn(List.of(template));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendTemplate(
                        44L,
                        new SendWhatsappTemplateRequest("retomar_contacto", "es_PE", List.of("Luis"))
                )
        );

        assertEquals("CRM_WHATSAPP_PARAMETROS_PLANTILLA_INVALIDOS", exception.getCode());
        verify(cloudApiClient, never()).sendTemplate(any(), any(), any(), any(), any());
    }

    private void mockOpenCustomerServiceWindow(CrmProspecto prospecto) {
        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setProspecto(prospecto);
        conversation.setUltimoEntranteEn(OffsetDateTime.now().minusMinutes(15));
        org.mockito.Mockito.lenient().when(conversationRepository.findByProspecto_Id(prospecto.getId())).thenReturn(Optional.of(conversation));
    }

    private CrmWhatsappConversationNote note(CrmWhatsappConversation conversation, int slot) {
        CrmWhatsappConversationNote note = new CrmWhatsappConversationNote();
        note.setConversation(conversation);
        note.setSlot(slot);
        note.setContenido("Nota " + slot);
        return note;
    }

    private String signature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
