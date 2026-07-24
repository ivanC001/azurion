package com.azurion.saascore.crm.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.SendWhatsappMessageRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappQuoteRequest;
import com.azurion.saascore.crm.application.dto.WhatsappWebhookResult;
import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.application.services.CrmLeadAssignmentService;
import com.azurion.saascore.crm.application.services.WhatsappIntegrationService;
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
import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
    private WhatsappCloudApiClient cloudApiClient;
    @Mock
    private CrmLeadAssignmentService leadAssignmentService;

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
                cloudApiClient,
                new ObjectMapper(),
                leadAssignmentService
        );
        config = new CrmCanalTokenConfig();
        config.setCanal("WHATSAPP");
        config.setActivo(true);
        config.setPhoneNumberId("1234567890");
        config.setAppSecret("encrypted-app-secret");
        config.setVerifyToken("encrypted-verify-token");
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

        var response = service.sendQuote(44L, 12L, new SendWhatsappQuoteRequest("Adjunto"));

        assertEquals("document", response.mensaje().tipoMensaje());
        assertEquals("wamid.quote-12", response.mensaje().metaMessageId());
        verify(updateCotizacionEstadoUseCase).execute(
                12L,
                new UpdateCotizacionEstadoRequest("ENVIADA", "WHATSAPP", null, null, null)
        );
        verify(actividadRepository).save(any());
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
