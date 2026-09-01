package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.CrmWhatsappConversationResponse;
import com.azurion.saascore.crm.application.dto.CrmWhatsappInternalNoteResponse;
import com.azurion.saascore.crm.application.dto.CrmWhatsappMessageResponse;
import com.azurion.saascore.crm.application.dto.CrmWhatsappTemplateResponse;
import com.azurion.saascore.crm.application.dto.SendWhatsappMessageRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappQuoteRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappQuoteResponse;
import com.azurion.saascore.crm.application.dto.SendWhatsappTemplateRequest;
import com.azurion.saascore.crm.application.dto.WhatsappUnreadSummaryResponse;
import com.azurion.saascore.crm.application.dto.WhatsappWebhookResult;
import com.azurion.saascore.crm.domain.entities.CrmActividad;
import com.azurion.saascore.crm.domain.WhatsappTemplate;
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
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient.SendResult;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.application.usecases.GenerateCotizacionPdfUseCase;
import com.azurion.saascore.cotizaciones.application.usecases.UpdateCotizacionEstadoUseCase;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class WhatsappIntegrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WhatsappIntegrationService.class);
    private static final String PUBLIC_WHATSAPP_OWNER = "crm-whatsapp";
    public static final String AUTOMATIC_WHATSAPP_OWNER = "crm-whatsapp-auto";
    private static final String AUTOMATIC_WHATSAPP_NAME = "Respuesta automatica";
    private static final long CUSTOMER_SERVICE_WINDOW_HOURS = 24;

    private final CrmCanalTokenConfigRepository configRepository;
    private final CrmProspectoRepository prospectoRepository;
    private final CrmActividadRepository actividadRepository;
    private final CrmWhatsappConversationRepository conversationRepository;
    private final CrmWhatsappConversationNoteRepository conversationNoteRepository;
    private final CrmWhatsappMessageRepository messageRepository;
    private final CotizacionRepository cotizacionRepository;
    private final GenerateCotizacionPdfUseCase generateCotizacionPdfUseCase;
    private final UpdateCotizacionEstadoUseCase updateCotizacionEstadoUseCase;
    private final CrmSecretEncryptionService secretEncryptionService;
    private final CrmPhoneNormalizationService phoneNormalizationService;
    private final WhatsappCloudApiClient cloudApiClient;
    private final ObjectMapper objectMapper;
    private final CrmLeadAssignmentService leadAssignmentService;
    private final WhatsappAutoReplyEnqueueService autoReplyEnqueueService;
    private final WhatsappOptOutService optOutService;
    private final CrmWhatsappReengagementOutboxRepository reengagementOutboxRepository;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public String verifyWebhook(String mode, String verifyToken, String challenge) {
        CrmCanalTokenConfig config = requireActiveConfig();
        String expectedToken = secretEncryptionService.decrypt(config.getVerifyToken());
        if (!"subscribe".equals(mode) || !secureEquals(expectedToken, verifyToken) || !hasText(challenge)) {
            throw new BusinessException("CRM_WHATSAPP_WEBHOOK_VERIFICACION_INVALIDA", "Meta no pudo verificar el webhook");
        }
        config.setWebhookVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        configRepository.save(config);
        return challenge;
    }

    @Transactional
    public WhatsappWebhookResult processWebhook(String rawBody, String signature) {
        CrmCanalTokenConfig config = requireActiveConfig();
        verifySignature(config, rawBody, signature);
        JsonNode root = parseJson(rawBody);
        if (!"whatsapp_business_account".equals(root.path("object").asText())) {
            return new WhatsappWebhookResult(0, 0, 0);
        }
        config.setLastWebhookAt(OffsetDateTime.now(ZoneOffset.UTC));

        Counters counters = new Counters();
        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                if (!"messages".equals(change.path("field").asText())) {
                    continue;
                }
                JsonNode value = change.path("value");
                validatePhoneNumberId(config, value.path("metadata").path("phone_number_id").asText(null));
                Map<String, String> contactNames = extractContactNames(value.path("contacts"));
                for (JsonNode message : value.path("messages")) {
                    processInboundMessage(config, message, contactNames, counters);
                }
                for (JsonNode status : value.path("statuses")) {
                    processStatus(status, counters);
                }
            }
        }
        if (counters.processed > 0) {
            config.setLastInboundMessageAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        return new WhatsappWebhookResult(counters.processed, counters.duplicates, counters.statuses);
    }

    @Transactional(readOnly = true)
    public List<CrmWhatsappMessageResponse> listMessages(Long prospectoId) {
        requireProspecto(prospectoId);
        List<CrmWhatsappMessage> recent = new ArrayList<>(
                messageRepository.findAllByProspecto_IdOrderByMensajeEnDescIdDesc(
                        prospectoId,
                        PageRequest.of(0, 200)
                )
        );
        Collections.reverse(recent);
        return recent.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmWhatsappConversationResponse> listConversations(String query,
                                                                   String estado,
                                                                   boolean soloNoLeidas,
                                                                   boolean soloMias) {
        String normalizedQuery = Optional.ofNullable(normalizeSearch(query)).orElse("");
        String normalizedStatus = hasText(estado) ? estado.trim().toUpperCase(Locale.ROOT) : null;
        String username = soloMias ? currentUser() : null;
        List<CrmWhatsappConversation> conversations = conversationRepository.searchRecent(
                normalizedQuery,
                normalizedStatus,
                soloNoLeidas,
                username,
                PageRequest.of(0, 100)
        );
        Map<Long, List<CrmWhatsappConversationNote>> notesByConversation = conversations.isEmpty()
                ? Map.of()
                : conversationNoteRepository
                        .findAllByConversation_IdInOrderByConversation_IdAscSlotAsc(
                                conversations.stream().map(CrmWhatsappConversation::getId).toList()
                        )
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                note -> note.getConversation().getId(),
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()
                        ));
        return conversations.stream()
                .map(item -> toConversationResponse(
                        item,
                        notesByConversation.getOrDefault(item.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public WhatsappUnreadSummaryResponse getUnreadSummary() {
        long unreadMessages = conversationRepository.sumUnreadMessages();
        long unreadConversations = conversationRepository.countByNoLeidosGreaterThan(0);
        CrmWhatsappConversation latest = conversationRepository
                .findFirstByNoLeidosGreaterThanOrderByUltimoMensajeEnDescIdDesc(0)
                .orElse(null);
        if (latest == null) {
            return new WhatsappUnreadSummaryResponse(0, 0, null, null, null, null);
        }
        return new WhatsappUnreadSummaryResponse(
                unreadMessages,
                unreadConversations,
                latest.getProspecto().getId(),
                latest.getProspecto().getNombre(),
                latest.getUltimoMensaje(),
                latest.getUltimoMensajeEn()
        );
    }

    public CrmWhatsappConversationResponse markConversationRead(Long prospectoId) {
        ReadReceiptWork work = Objects.requireNonNull(transactionTemplate.execute(status -> {
            CrmWhatsappConversation conversation = requireConversation(prospectoId);
            OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC);
            List<CrmWhatsappMessage> unreadMessages = messageRepository
                    .findAllByProspecto_IdAndDireccionAndLeidoEnIsNull(prospectoId, "ENTRANTE");
            unreadMessages.forEach(message -> message.setLeidoEn(readAt));
            messageRepository.saveAll(unreadMessages);
            conversation.setNoLeidos(0);
            CrmWhatsappConversation saved = conversationRepository.save(conversation);
            CrmWhatsappMessage latest = messageRepository
                    .findFirstByProspecto_IdAndDireccionOrderByMensajeEnDescIdDesc(prospectoId, "ENTRANTE")
                    .orElse(null);
            CrmCanalTokenConfig config = latest == null ? null : requireActiveConfig();
            return new ReadReceiptWork(
                    toConversationResponse(saved),
                    config,
                    latest == null ? null : latest.getMetaMessageId()
            );
        }));

        if (work.metaMessageId() != null) {
            try {
                // Network I/O deliberately runs after the database transaction,
                // so a slow Meta response cannot retain a pooled connection.
                cloudApiClient.markAsRead(work.config(), work.metaMessageId());
            } catch (BusinessException ex) {
                log.warn("No se pudo confirmar lectura en Meta para wamid={}: {}", work.metaMessageId(), ex.getCode());
            }
        }
        return work.response();
    }

    @Transactional
    public CrmWhatsappConversationResponse updateConversationStatus(Long prospectoId, String estado) {
        CrmWhatsappConversation conversation = requireConversation(prospectoId);
        conversation.setEstado(estado.trim().toUpperCase(Locale.ROOT));
        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public CrmWhatsappConversationResponse assignConversation(Long prospectoId, String responsableId) {
        CrmWhatsappConversation conversation = requireConversation(prospectoId);
        String owner = trimToNull(responsableId);
        conversation.setResponsableId(owner);
        if (owner != null) {
            CrmProspecto prospecto = conversation.getProspecto();
            prospecto.setResponsableId(owner);
            prospectoRepository.save(prospecto);
        }
        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public CrmWhatsappConversationResponse updateConversationNote(Long prospectoId, String note) {
        CrmWhatsappConversation conversation = requireConversationForUpdate(prospectoId);
        String content = trimToNull(note);
        List<CrmWhatsappConversationNote> notes =
                conversationNoteRepository.findAllByConversation_IdOrderBySlotAsc(conversation.getId());
        CrmWhatsappConversationNote first = notes.stream()
                .filter(item -> item.getSlot() == 1)
                .findFirst()
                .orElse(null);
        if (content == null) {
            if (first != null) {
                conversationNoteRepository.delete(first);
            }
        } else if (first == null) {
            first = new CrmWhatsappConversationNote();
            first.setConversation(conversation);
            first.setSlot(1);
            first.setContenido(content);
            conversationNoteRepository.save(first);
        } else {
            first.setContenido(content);
            conversationNoteRepository.save(first);
        }
        conversation.setNotaInterna(content);
        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public CrmWhatsappConversationResponse createConversationNote(Long prospectoId, String note) {
        CrmWhatsappConversation conversation = requireConversationForUpdate(prospectoId);
        List<CrmWhatsappConversationNote> notes =
                conversationNoteRepository.findAllByConversation_IdOrderBySlotAsc(conversation.getId());
        Set<Integer> occupiedSlots = notes.stream()
                .map(CrmWhatsappConversationNote::getSlot)
                .collect(java.util.stream.Collectors.toSet());
        int availableSlot = java.util.stream.IntStream.rangeClosed(1, 3)
                .filter(slot -> !occupiedSlots.contains(slot))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_NOTAS_LIMITE",
                        "Solo puedes guardar hasta 3 notas internas por conversacion"
                ));
        CrmWhatsappConversationNote savedNote = new CrmWhatsappConversationNote();
        savedNote.setConversation(conversation);
        savedNote.setSlot(availableSlot);
        savedNote.setContenido(note.trim());
        conversationNoteRepository.save(savedNote);
        syncLegacyNote(conversation);
        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public CrmWhatsappConversationResponse updateSavedConversationNote(Long prospectoId, Long noteId, String note) {
        CrmWhatsappConversation conversation = requireConversationForUpdate(prospectoId);
        CrmWhatsappConversationNote savedNote = requireConversationNote(conversation.getId(), noteId);
        savedNote.setContenido(note.trim());
        conversationNoteRepository.save(savedNote);
        syncLegacyNote(conversation);
        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public CrmWhatsappConversationResponse deleteConversationNote(Long prospectoId, Long noteId) {
        CrmWhatsappConversation conversation = requireConversationForUpdate(prospectoId);
        CrmWhatsappConversationNote savedNote = requireConversationNote(conversation.getId(), noteId);
        conversationNoteRepository.delete(savedNote);
        conversationNoteRepository.flush();
        syncLegacyNote(conversation);
        return toConversationResponse(conversationRepository.save(conversation));
    }

    public CrmWhatsappMessageResponse sendMessage(Long prospectoId, SendWhatsappMessageRequest request) {
        CrmProspecto prospecto = requireProspecto(prospectoId);
        assertCustomerServiceWindowOpen(prospectoId);
        CrmCanalTokenConfig config = requireActiveConfig();
        String recipient = normalizePhone(prospecto.getTelefono(), prospecto.getPaisCodigo());
        String body = request.mensaje().trim();
        SendResult sendResult = cloudApiClient.sendText(config, recipient, body, Boolean.TRUE.equals(request.previewUrl()));
        String actor = currentUser();

        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            CrmWhatsappMessage message = new CrmWhatsappMessage();
            message.setProspecto(prospecto);
            message.setMetaMessageId(sendResult.metaMessageId());
            message.setDireccion("SALIENTE");
            message.setRemitente(config.getPhoneNumberId());
            message.setDestinatario(sendResult.whatsappId());
            message.setTipoMensaje("text");
            message.setContenido(body);
            message.setEstado("ENVIADO");
            message.setMensajeEn(OffsetDateTime.now(ZoneOffset.UTC));
            message.setRawPayload(sendResult.rawResponse());
            message.setEnviadoPorUsuarioId(actor);
            message.setEnviadoPorNombre(actor);
            CrmWhatsappMessage saved = messageRepository.save(message);
            updateConversation(prospecto, saved, false);
            createWhatsappActivity(prospecto, body, saved.getMensajeEn(), false, actor);
            return toResponse(saved);
        }));
    }

    public CrmWhatsappMessageResponse sendAutomaticMessage(Long prospectoId, String body) {
        CrmProspecto prospecto = requireProspecto(prospectoId);
        assertCustomerServiceWindowOpen(prospectoId);
        CrmCanalTokenConfig config = requireActiveConfig();
        String recipient = normalizePhone(prospecto.getTelefono(), prospecto.getPaisCodigo());
        String normalizedBody = truncate(body, 4096);
        SendResult sendResult = cloudApiClient.sendText(config, recipient, normalizedBody, false);

        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            CrmWhatsappMessage message = new CrmWhatsappMessage();
            message.setProspecto(prospecto);
            message.setMetaMessageId(sendResult.metaMessageId());
            message.setDireccion("SALIENTE");
            message.setRemitente(config.getPhoneNumberId());
            message.setDestinatario(sendResult.whatsappId());
            message.setTipoMensaje("text");
            message.setContenido(normalizedBody);
            message.setEstado("ENVIADO");
            message.setMensajeEn(OffsetDateTime.now(ZoneOffset.UTC));
            message.setRawPayload(sendResult.rawResponse());
            message.setEnviadoPorUsuarioId(AUTOMATIC_WHATSAPP_OWNER);
            message.setEnviadoPorNombre(AUTOMATIC_WHATSAPP_NAME);
            CrmWhatsappMessage saved = messageRepository.save(message);
            updateConversation(prospecto, saved, false);
            createWhatsappActivity(
                    prospecto,
                    normalizedBody,
                    saved.getMensajeEn(),
                    false,
                    AUTOMATIC_WHATSAPP_OWNER
            );
            return toResponse(saved);
        }));
    }

    public List<CrmWhatsappTemplateResponse> listApprovedTemplates() {
        CrmCanalTokenConfig config = requireActiveConfig();
        return cloudApiClient.listApprovedTemplates(config).stream()
                .map(t -> new CrmWhatsappTemplateResponse(
                        t.name(),
                        t.languageCode(),
                        t.category(),
                        t.bodyText(),
                        t.parameterCount(),
                        t.id(),
                        t.status(),
                        t.available(),
                        t.unavailableReason(),
                        t.components().stream().map(c -> new CrmWhatsappTemplateResponse.Componente(
                                c.type(), c.text(), c.parameters())).toList()
                ))
                .toList();
    }

    public CrmWhatsappMessageResponse sendTemplate(Long prospectoId, SendWhatsappTemplateRequest request) {
        CrmProspecto prospecto = requireProspecto(prospectoId);
        requireConversation(prospectoId);
        CrmCanalTokenConfig config = requireActiveConfig();
        String recipient = normalizePhone(prospecto.getTelefono(), prospecto.getPaisCodigo());

        WhatsappTemplate matched = requireSendableTemplate(request.nombre(), request.idioma());

        List<String> params = matched.validateParameters(request.parametros());
        String renderedBody = matched.render(params);
        var parameterSnapshot = objectMapper.createArrayNode();
        List<WhatsappTemplate.Variable> variables = matched.variables();
        for (int index = 0; index < variables.size(); index++) {
            var variable = variables.get(index);
            parameterSnapshot.addObject().put("componente", variable.component())
                    .put("variable", variable.name()).put("valor", params.get(index));
        }

        SendResult sendResult = cloudApiClient.sendTemplate(
                config,
                recipient,
                matched,
                params
        );

        String actor = currentUser();

        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            CrmWhatsappMessage message = new CrmWhatsappMessage();
            message.setProspecto(prospecto);
            message.setMetaMessageId(sendResult.metaMessageId());
            message.setDireccion("SALIENTE");
            message.setRemitente(config.getPhoneNumberId());
            message.setDestinatario(sendResult.whatsappId());
            message.setTipoMensaje("template");
            message.setPlantillaNombre(matched.name());
            message.setPlantillaIdioma(matched.languageCode());
            message.setPlantillaParametrosJson(parameterSnapshot.toString());
            message.setContenido(renderedBody);
            message.setEstado("ENVIADO");
            message.setMensajeEn(OffsetDateTime.now(ZoneOffset.UTC));
            message.setRawPayload(sendResult.rawResponse());
            message.setEnviadoPorUsuarioId(actor);
            message.setEnviadoPorNombre(actor);
            CrmWhatsappMessage saved = messageRepository.save(message);
            updateConversation(prospecto, saved, false);
            createWhatsappActivity(prospecto, renderedBody, saved.getMensajeEn(), false, actor);
            return toResponse(saved);
        }));
    }

    /**
     * Resuelve una plantilla aprobada y enviable del WABA del tenant.
     *
     * <p>Lo usa tanto el envio inmediato como la programacion de reenganches: conviene
     * fallar al programar y no una semana despues, cuando ya no hay nadie mirando.
     */
    public WhatsappTemplate requireSendableTemplate(String nombre, String idioma) {
        return cloudApiClient.listApprovedTemplates(requireActiveConfig()).stream()
                .filter(template -> template.name().equals(nombre)
                        && template.languageCode().equals(idioma))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_PLANTILLA_NO_ENCONTRADA",
                        "La plantilla solicitada no existe o no esta aprobada"
                ));
    }

    private void assertCustomerServiceWindowOpen(Long prospectoId) {
        OffsetDateTime lastInbound = conversationRepository.findByProspecto_Id(prospectoId)
                .map(CrmWhatsappConversation::getUltimoEntranteEn).orElse(null);
        if (lastInbound == null || !OffsetDateTime.now(ZoneOffset.UTC)
                .isBefore(lastInbound.plusHours(CUSTOMER_SERVICE_WINDOW_HOURS))) {
            throw new BusinessException("CRM_WHATSAPP_VENTANA_ATENCION_CERRADA",
                    "La ventana de atencion de 24 horas esta cerrada. Envia una plantilla aprobada y espera la respuesta del cliente.");
        }
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponse> listProspectQuotes(Long prospectoId) {
        requireProspecto(prospectoId);
        return CotizacionMapper.toResponses(cotizacionRepository.findAllByCrmProspectoId(prospectoId));
    }

    public SendWhatsappQuoteResponse sendQuote(
            Long prospectoId,
            Long quoteId,
            SendWhatsappQuoteRequest request) {
        CrmProspecto prospecto = requireProspecto(prospectoId);
        assertCustomerServiceWindowOpen(prospectoId);
        Cotizacion quote = cotizacionRepository.findByIdAndCrmProspectoId(quoteId, prospectoId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_COTIZACION_NO_ENCONTRADA",
                        "La cotizacion no pertenece a este prospecto"
                ));
        String sendToken = UUID.randomUUID().toString();
        claimQuoteSend(quoteId, sendToken);
        boolean metaRequestStarted = false;
        boolean deliveryConfirmed = false;
        try {
            CrmCanalTokenConfig config = requireActiveConfig();
            String recipient = normalizePhone(prospecto.getTelefono(), prospecto.getPaisCodigo());
            String caption = trimToNull(request.mensaje());
            if (caption == null) {
                caption = "Hola " + firstNonBlank(prospecto.getNombre(), "")
                        + ", adjuntamos la cotizacion COT-" + String.format(Locale.ROOT, "%06d", quote.getId()) + ".";
            }

            CotizacionPdfResponse pdf = generateCotizacionPdfUseCase.execute(quote.getId());
            byte[] pdfBytes;
            try {
                pdfBytes = Base64.getDecoder().decode(pdf.base64());
            } catch (IllegalArgumentException exception) {
                throw BusinessException.internal(
                        "CRM_WHATSAPP_COTIZACION_PDF_INVALIDA",
                        "No se pudo preparar el PDF de la cotizacion"
                );
            }
            String mediaId = cloudApiClient.uploadMedia(
                    config,
                    pdfBytes,
                    pdf.fileName(),
                    pdf.contentType()
            );
            metaRequestStarted = true;
            SendResult sendResult = cloudApiClient.sendDocument(
                    config,
                    recipient,
                    mediaId,
                    pdf.fileName(),
                    caption
            );
            if (cotizacionRepository.markWhatsappSent(
                    quoteId,
                    sendToken,
                    sendResult.metaMessageId(),
                    LocalDateTime.now()
            ) != 1) {
                throw BusinessException.internal(
                        "CRM_WHATSAPP_COTIZACION_LEASE_PERDIDO",
                        "Meta recibio la cotizacion, pero no se pudo confirmar el bloqueo del envio."
                );
            }
            deliveryConfirmed = true;

            String actor = currentUser();
            CrmWhatsappMessage message = new CrmWhatsappMessage();
            message.setProspecto(prospecto);
            message.setMetaMessageId(sendResult.metaMessageId());
            message.setDireccion("SALIENTE");
            message.setRemitente(config.getPhoneNumberId());
            message.setDestinatario(sendResult.whatsappId());
            message.setTipoMensaje("document");
            message.setContenido(caption);
            message.setEstado("ENVIADO");
            message.setMensajeEn(OffsetDateTime.now(ZoneOffset.UTC));
            message.setRawPayload(sendResult.rawResponse());
            message.setEnviadoPorUsuarioId(actor);
            message.setEnviadoPorNombre(actor);
            CrmWhatsappMessage saved = messageRepository.save(message);
            updateConversation(prospecto, saved, false);
            createWhatsappActivity(prospecto, caption, saved.getMensajeEn(), false, actor);

            CotizacionResponse updatedQuote = updateCotizacionEstadoUseCase.execute(
                    quoteId,
                    new UpdateCotizacionEstadoRequest("ENVIADA", "WHATSAPP", null, null, null)
            );
            return new SendWhatsappQuoteResponse(toResponse(saved), updatedQuote);
        } catch (RuntimeException error) {
            if (!deliveryConfirmed) {
                if (metaRequestStarted) {
                    cotizacionRepository.markWhatsappUncertain(
                            quoteId,
                            sendToken,
                            trimSendError(error),
                            LocalDateTime.now()
                    );
                } else {
                    cotizacionRepository.markWhatsappFailed(
                            quoteId,
                            sendToken,
                            trimSendError(error),
                            LocalDateTime.now()
                    );
                }
            }
            throw error;
        }
    }

    private void claimQuoteSend(Long quoteId, String sendToken) {
        if (cotizacionRepository.claimWhatsappSend(
                quoteId,
                sendToken,
                OffsetDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now()
        ) == 1) {
            return;
        }
        Cotizacion quote = cotizacionRepository.findById(quoteId)
                .orElseThrow(() -> BusinessException.notFound(
                        "CRM_WHATSAPP_COTIZACION_NO_ENCONTRADA",
                        "La cotizacion no existe"
                ));
        if ("SENT".equals(quote.getWhatsappSendStatus())) {
            throw BusinessException.conflict(
                    "CRM_WHATSAPP_COTIZACION_YA_ENVIADA",
                    "La cotizacion ya fue enviada por WhatsApp. No se realizo un segundo envio."
            );
        }
        if ("UNKNOWN".equals(quote.getWhatsappSendStatus())) {
            throw BusinessException.conflict(
                    "CRM_WHATSAPP_COTIZACION_ESTADO_INCIERTO",
                    "El envio anterior tiene un resultado incierto. Revisa la conversacion antes de reenviar."
            );
        }
        throw BusinessException.conflict(
                "CRM_WHATSAPP_COTIZACION_EN_PROCESO",
                "La cotizacion ya se esta enviando por WhatsApp. Espera la confirmacion."
        );
    }

    private String trimSendError(RuntimeException error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private void processInboundMessage(CrmCanalTokenConfig config,
                                       JsonNode messageNode,
                                       Map<String, String> contactNames,
                                       Counters counters) {
        String metaMessageId = text(messageNode, "id");
        if (!hasText(metaMessageId)) {
            return;
        }
        if (messageRepository.existsByMetaMessageId(metaMessageId)) {
            counters.duplicates++;
            return;
        }

        String sender = normalizePhone(text(messageNode, "from"), null);
        String type = firstNonBlank(text(messageNode, "type"), "unknown");
        String body = extractMessageBody(messageNode, type);
        OffsetDateTime messageTime = parseTimestamp(text(messageNode, "timestamp"));
        String contactName = firstNonBlank(contactNames.get(sender), sender);
        CrmProspecto prospecto = findOrCreateProspecto(sender, contactName, body, type, metaMessageId, messageTime);

        CrmWhatsappMessage message = new CrmWhatsappMessage();
        message.setProspecto(prospecto);
        message.setMetaMessageId(metaMessageId);
        message.setDireccion("ENTRANTE");
        message.setRemitente(sender);
        message.setDestinatario(config.getPhoneNumberId());
        message.setTipoMensaje(truncate(type, 40));
        message.setContenido(body);
        message.setEstado("RECIBIDO");
        message.setMensajeEn(messageTime);
        message.setRawPayload(messageNode.toString());
        CrmWhatsappMessage saved = messageRepository.save(message);
        updateConversation(prospecto, saved, true);
        createWhatsappActivity(prospecto, body, messageTime, true, PUBLIC_WHATSAPP_OWNER);
        // Una respuesta del cliente reabre la ventana: los reenganches que quedaban
        // programados ya no hacen falta y gastarian una plantilla al pedo.
        reengagementOutboxRepository.cancelPendingForProspecto(
                TenantContext.getTenantId(),
                prospecto.getId(),
                "El cliente respondio y la ventana de 24 horas se reabrio",
                LocalDateTime.now()
        );
        if (optOutService.applyIfRequested(prospecto, body)) {
            log.info("El prospecto {} pidio la baja de WhatsApp", prospecto.getId());
        }
        autoReplyEnqueueService.enqueueIfEnabled(saved);
        counters.processed++;
    }

    private void processStatus(JsonNode statusNode, Counters counters) {
        String metaMessageId = text(statusNode, "id");
        if (!hasText(metaMessageId)) {
            return;
        }
        messageRepository.findByMetaMessageId(metaMessageId).ifPresent(message -> {
            String nextStatus = normalizeStatus(text(statusNode, "status"));
            List<String> statusOrder = List.of("ENVIADO", "FALLIDO", "ENTREGADO", "LEIDO", "ELIMINADO");
            if (statusOrder.indexOf(nextStatus) < 0
                    || statusOrder.indexOf(nextStatus) < statusOrder.indexOf(message.getEstado())) {
                return;
            }
            message.setEstado(nextStatus);
            if ("FALLIDO".equals(nextStatus)) {
                JsonNode error = statusNode.path("errors").path(0);
                message.setErrorCodigo(truncate(error.path("code").asText(null), 80));
                String detail = firstNonBlank(error.path("error_data").path("details").asText(null),
                        error.path("message").asText(null), error.path("title").asText(null),
                        "Meta no pudo entregar el mensaje");
                message.setErrorDetalle(truncate(detail.replaceAll(
                        "(?i)(access[_ -]?token|secret|authorization)[=: ]+\\S+", "$1=***"), 500));
            } else {
                message.setErrorCodigo(null);
                message.setErrorDetalle(null);
            }
            message.setRawPayload(statusNode.toString());
            messageRepository.save(message);
            counters.statuses++;
        });
    }

    private CrmProspecto findOrCreateProspecto(String sender,
                                               String contactName,
                                               String body,
                                               String type,
                                               String metaMessageId,
                                               OffsetDateTime messageTime) {
        CrmProspecto prospecto = prospectoRepository.findFirstByTelefonoNormalizado(sender).orElseGet(CrmProspecto::new);
        boolean isNew = prospecto.getId() == null;
        if (isNew) {
            prospecto.setTipoPersona("SIN_DEFINIR");
            prospecto.setNombre(truncate(firstNonBlank(contactName, sender), 180));
            prospecto.setTelefono(sender);
            prospecto.setPaisCodigo(phoneNormalizationService.countryCodeForPhone(sender));
            prospecto.setOrigen("WHATSAPP");
            prospecto.setCanalIngreso("WHATSAPP");
            prospecto.setCampania("WhatsApp");
            prospecto.setTipoInteres("PRODUCTO");
            prospecto.setInteresPrincipal("Consulta por WhatsApp");
            prospecto.setInteresDetalle(truncate(body, 1500));
            prospecto.setProductoPendiente(true);
            prospecto.setEstado("NUEVO");
            prospecto.setNivelInteres("FRIO");
            prospecto.setInteresReal("BAJO");
            prospecto.setPresupuestoDefinido("DESCONOCIDO");
            prospecto.setTomadorDecision("DESCONOCIDO");
            prospecto.setFechaEstimadaCompra("DESCONOCIDO");
            prospecto.setScoreCalificacion(0);
            prospecto.setTemperatura("FRIO");
            leadAssignmentService.assignAutomatically(prospecto, PUBLIC_WHATSAPP_OWNER);
            prospecto.setMetadataJson(buildLeadMetadata(type, metaMessageId, messageTime));
        } else if (hasText(contactName)
                && sender.equals(prospecto.getNombre())
                && !sender.equals(contactName)) {
            prospecto.setNombre(truncate(contactName, 180));
        }

        prospecto.setMensaje(truncate(body, 1500));
        prospecto.setFechaInteres(messageTime.toLocalDate());
        prospecto.setObservacion(truncate(body, 1000));
        return prospectoRepository.save(prospecto);
    }

    private void createWhatsappActivity(CrmProspecto prospecto,
                                        String body,
                                        OffsetDateTime messageTime,
                                        boolean inbound,
                                        String actorId) {
        CrmActividad activity = new CrmActividad();
        activity.setProspecto(prospecto);
        activity.setTipoActividad("WHATSAPP");
        activity.setAsunto(truncate(
                (inbound ? "WhatsApp recibido de " : "WhatsApp enviado a ") + prospecto.getNombre(),
                220
        ));
        activity.setDescripcion(truncate(body, 1000));
        activity.setFechaProgramada(messageTime);
        activity.setFechaRealizada(messageTime);
        activity.setEstado("REALIZADA");
        activity.setUsuarioId(actorId);
        activity.setResultado(inbound ? "Mensaje recibido por WhatsApp" : "Mensaje enviado por WhatsApp");
        activity.setResultadoContacto("CONTACTADO");
        activity.setEstadoProspectoResultado(prospecto.getEstado());
        activity.setNivelInteres(prospecto.getNivelInteres());
        actividadRepository.save(activity);
    }

    private String extractMessageBody(JsonNode message, String type) {
        String body = switch (type) {
            case "text" -> message.path("text").path("body").asText(null);
            case "button" -> message.path("button").path("text").asText(null);
            case "interactive" -> firstNonBlank(
                    message.path("interactive").path("button_reply").path("title").asText(null),
                    message.path("interactive").path("list_reply").path("title").asText(null)
            );
            case "image" -> message.path("image").path("caption").asText(null);
            case "document" -> firstNonBlank(
                    message.path("document").path("caption").asText(null),
                    message.path("document").path("filename").asText(null)
            );
            case "video" -> message.path("video").path("caption").asText(null);
            case "location" -> "Ubicacion: "
                    + message.path("location").path("latitude").asText("") + ","
                    + message.path("location").path("longitude").asText("");
            default -> null;
        };
        return truncate(firstNonBlank(body, "[Mensaje " + type + "]"), 4096);
    }

    private Map<String, String> extractContactNames(JsonNode contacts) {
        Map<String, String> names = new HashMap<>();
        for (JsonNode contact : contacts) {
            String whatsappId = digits(text(contact, "wa_id"));
            if (hasText(whatsappId)) {
                names.put(whatsappId, contact.path("profile").path("name").asText(whatsappId));
            }
        }
        return names;
    }

    private void validatePhoneNumberId(CrmCanalTokenConfig config, String payloadPhoneNumberId) {
        if (!secureEquals(config.getPhoneNumberId(), payloadPhoneNumberId)) {
            throw new BusinessException(
                    "CRM_WHATSAPP_PHONE_ID_INVALIDO",
                    "El webhook no corresponde al Phone number ID configurado para este tenant"
            );
        }
    }

    private void verifySignature(CrmCanalTokenConfig config, String rawBody, String signature) {
        String appSecret = secretEncryptionService.decrypt(config.getAppSecret());
        if (!hasText(appSecret) || !hasText(signature) || !signature.startsWith("sha256=")) {
            throw new BusinessException("CRM_WHATSAPP_FIRMA_REQUERIDA", "Falta la firma X-Hub-Signature-256 de Meta");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.trim().getBytes(StandardCharsets.US_ASCII))) {
                throw new BusinessException("CRM_WHATSAPP_FIRMA_INVALIDA", "La firma del webhook de Meta no es valida");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("CRM_WHATSAPP_FIRMA_ERROR", "No se pudo validar la firma del webhook");
        }
    }

    private void updateConversation(CrmProspecto prospecto, CrmWhatsappMessage message, boolean inbound) {
        CrmWhatsappConversation conversation = conversationRepository.findByProspecto_Id(prospecto.getId())
                .orElseGet(() -> newConversation(prospecto));
        conversation.setEstado("ABIERTA");
        conversation.setUltimoMensaje(message.getContenido());
        conversation.setUltimaDireccion(message.getDireccion());
        conversation.setUltimoMensajeEn(message.getMensajeEn());
        if (inbound) {
            if (conversation.getUltimoEntranteEn() == null
                    || message.getMensajeEn().isAfter(conversation.getUltimoEntranteEn())) {
                conversation.setUltimoEntranteEn(message.getMensajeEn());
            }
            conversation.setNoLeidos(safeUnreadCount(conversation) + 1);
        }
        conversationRepository.save(conversation);
    }

    private CrmWhatsappConversation newConversation(CrmProspecto prospecto) {
        CrmWhatsappConversation conversation = new CrmWhatsappConversation();
        conversation.setProspecto(prospecto);
        conversation.setEstado("ABIERTA");
        conversation.setNoLeidos(0);
        if (hasText(prospecto.getResponsableId()) && !PUBLIC_WHATSAPP_OWNER.equals(prospecto.getResponsableId())) {
            conversation.setResponsableId(prospecto.getResponsableId());
        }
        return conversation;
    }

    private CrmWhatsappConversation requireConversation(Long prospectoId) {
        return conversationRepository.findByProspecto_Id(prospectoId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_CONVERSACION_NO_ENCONTRADA",
                        "El prospecto aun no tiene una conversacion de WhatsApp"
                ));
    }

    private CrmWhatsappConversation requireConversationForUpdate(Long prospectoId) {
        return conversationRepository.findForUpdateByProspectoId(prospectoId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_CONVERSACION_NO_ENCONTRADA",
                        "El prospecto aun no tiene una conversacion de WhatsApp"
                ));
    }

    private CrmWhatsappConversationNote requireConversationNote(Long conversationId, Long noteId) {
        return conversationNoteRepository.findByIdAndConversation_Id(noteId, conversationId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_NOTA_NO_ENCONTRADA",
                        "La nota interna no pertenece a esta conversacion"
                ));
    }

    private void syncLegacyNote(CrmWhatsappConversation conversation) {
        String firstContent = conversationNoteRepository
                .findAllByConversation_IdOrderBySlotAsc(conversation.getId())
                .stream()
                .findFirst()
                .map(CrmWhatsappConversationNote::getContenido)
                .orElse(null);
        conversation.setNotaInterna(firstContent);
    }

    private boolean matchesConversation(CrmWhatsappConversation conversation, String query) {
        if (query == null) {
            return true;
        }
        CrmProspecto prospecto = conversation.getProspecto();
        return contains(prospecto.getNombre(), query)
                || contains(prospecto.getTelefono(), query)
                || contains(prospecto.getCorreo(), query)
                || contains(prospecto.getCampania(), query)
                || contains(prospecto.getInteresPrincipal(), query)
                || contains(conversation.getUltimoMensaje(), query);
    }

    private CrmWhatsappConversationResponse toConversationResponse(CrmWhatsappConversation conversation) {
        return toConversationResponse(
                conversation,
                conversationNoteRepository.findAllByConversation_IdOrderBySlotAsc(conversation.getId())
        );
    }

    private CrmWhatsappConversationResponse toConversationResponse(
            CrmWhatsappConversation conversation,
            List<CrmWhatsappConversationNote> conversationNotes
    ) {
        CrmProspecto prospecto = conversation.getProspecto();
        List<CrmWhatsappInternalNoteResponse> notes = conversationNotes.stream()
                .map(note -> new CrmWhatsappInternalNoteResponse(
                        note.getId(),
                        note.getSlot(),
                        note.getContenido(),
                        note.getCreatedAt(),
                        note.getUpdatedAt()
                ))
                .toList();
        String legacyNote = notes.isEmpty() ? conversation.getNotaInterna() : notes.getFirst().contenido();
        OffsetDateTime lastInbound = conversation.getUltimoEntranteEn();
        OffsetDateTime ventanaAtencionHasta = lastInbound == null ? null : lastInbound.plusHours(CUSTOMER_SERVICE_WINDOW_HOURS);
        boolean ventanaAbierta = lastInbound != null && OffsetDateTime.now(ZoneOffset.UTC).isBefore(ventanaAtencionHasta);

        return new CrmWhatsappConversationResponse(
                conversation.getId(),
                prospecto.getId(),
                prospecto.getNombre(),
                prospecto.getTelefono(),
                prospecto.getCorreo(),
                prospecto.getDireccion(),
                prospecto.getOrigen(),
                prospecto.getCanalIngreso(),
                prospecto.getCampania(),
                prospecto.getInteresPrincipal(),
                prospecto.getEstado(),
                prospecto.getNivelInteres(),
                conversation.getResponsableId(),
                conversation.getEstado(),
                safeUnreadCount(conversation),
                conversation.getUltimoMensaje(),
                conversation.getUltimaDireccion(),
                conversation.getUltimoMensajeEn(),
                conversation.getUltimoEntranteEn(),
                ventanaAtencionHasta,
                ventanaAbierta,
                legacyNote,
                notes
        );
    }

    private int safeUnreadCount(CrmWhatsappConversation conversation) {
        return conversation.getNoLeidos() == null ? 0 : conversation.getNoLeidos();
    }

    private CrmCanalTokenConfig requireActiveConfig() {
        CrmCanalTokenConfig config = configRepository.findByCanal("WHATSAPP")
                .orElseThrow(() -> new BusinessException("CRM_WHATSAPP_NO_CONFIGURADO", "WhatsApp no esta configurado para este tenant"));
        if (!config.isActivo()) {
            throw new BusinessException("CRM_WHATSAPP_INACTIVO", "La integracion de WhatsApp esta inactiva");
        }
        return config;
    }

    private CrmProspecto requireProspecto(Long prospectoId) {
        return prospectoRepository.findById(prospectoId)
                .orElseThrow(() -> new BusinessException("CRM_PROSPECTO_NO_ENCONTRADO", "Prospecto CRM no encontrado"));
    }

    private CrmWhatsappMessageResponse toResponse(CrmWhatsappMessage message) {
        return new CrmWhatsappMessageResponse(
                message.getId(),
                message.getProspecto() == null ? null : message.getProspecto().getId(),
                message.getMetaMessageId(),
                message.getDireccion(),
                message.getRemitente(),
                message.getDestinatario(),
                message.getTipoMensaje(),
                message.getContenido(),
                message.getEstado(),
                message.getMensajeEn(),
                message.getLeidoEn(),
                message.getEnviadoPorUsuarioId(),
                message.getEnviadoPorNombre(),
                message.getErrorCodigo(),
                message.getErrorDetalle(),
                message.getCreatedAt(),
                message.getPlantillaNombre(),
                message.getPlantillaIdioma()
        );
    }

    private String buildLeadMetadata(String type, String metaMessageId, OffsetDateTime messageTime) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("source", "whatsapp-cloud-api");
        metadata.put("messageType", type);
        metadata.put("metaMessageId", metaMessageId);
        metadata.put("receivedAt", messageTime.toString());
        return metadata.toString();
    }

    private JsonNode parseJson(String value) {
        try {
            JsonNode root = objectMapper.readTree(value == null ? "" : value);
            if (root == null || !root.isObject()) {
                throw new BusinessException("CRM_WHATSAPP_PAYLOAD_INVALIDO", "El webhook de WhatsApp no contiene un objeto JSON");
            }
            return root;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("CRM_WHATSAPP_PAYLOAD_INVALIDO", "El webhook de WhatsApp no contiene JSON valido");
        }
    }

    private OffsetDateTime parseTimestamp(String value) {
        try {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(value)), ZoneOffset.UTC);
        } catch (Exception ex) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    private String normalizeStatus(String value) {
        return switch (firstNonBlank(value, "unknown").toLowerCase()) {
            case "sent" -> "ENVIADO";
            case "delivered" -> "ENTREGADO";
            case "read" -> "LEIDO";
            case "failed" -> "FALLIDO";
            case "deleted" -> "ELIMINADO";
            default -> truncate(value == null ? "DESCONOCIDO" : value.toUpperCase(), 30);
        };
    }

    private String normalizePhone(String value, String paisCodigo) {
        String normalized = phoneNormalizationService.normalize(value, paisCodigo).identity();
        if (!hasText(normalized) || normalized.length() < 8 || normalized.length() > 15) {
            throw new BusinessException(
                    "CRM_WHATSAPP_TELEFONO_INVALIDO",
                    "El prospecto debe tener un telefono con codigo de pais para usar WhatsApp"
            );
        }
        return normalized;
    }

    private String digits(String value) {
        return value == null ? null : value.replaceAll("[^0-9]", "");
    }

    private String normalizeSearch(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? PUBLIC_WHATSAPP_OWNER
                : truncate(authentication.getName(), 80);
    }

    private String text(JsonNode node, String field) {
        return node == null ? null : node.path(field).asText(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ReadReceiptWork(
            CrmWhatsappConversationResponse response,
            CrmCanalTokenConfig config,
            String metaMessageId
    ) {
    }

    private static final class Counters {
        private int processed;
        private int duplicates;
        private int statuses;
    }
}
