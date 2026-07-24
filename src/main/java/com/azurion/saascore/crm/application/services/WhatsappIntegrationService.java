package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CrmWhatsappConversationResponse;
import com.azurion.saascore.crm.application.dto.CrmWhatsappInternalNoteResponse;
import com.azurion.saascore.crm.application.dto.CrmWhatsappMessageResponse;
import com.azurion.saascore.crm.application.dto.SendWhatsappMessageRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappQuoteRequest;
import com.azurion.saascore.crm.application.dto.SendWhatsappQuoteResponse;
import com.azurion.saascore.crm.application.dto.WhatsappUnreadSummaryResponse;
import com.azurion.saascore.crm.application.dto.WhatsappWebhookResult;
import com.azurion.saascore.crm.domain.entities.CrmActividad;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsappIntegrationService {

    private static final String PUBLIC_WHATSAPP_OWNER = "crm-whatsapp";

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
    private final WhatsappCloudApiClient cloudApiClient;
    private final ObjectMapper objectMapper;
    private final CrmLeadAssignmentService leadAssignmentService;

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
        return messageRepository.findAllByProspecto_IdOrderByMensajeEnAscIdAsc(prospectoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmWhatsappConversationResponse> listConversations(String query,
                                                                   String estado,
                                                                   boolean soloNoLeidas,
                                                                   boolean soloMias) {
        String normalizedQuery = normalizeSearch(query);
        String normalizedStatus = hasText(estado) ? estado.trim().toUpperCase(Locale.ROOT) : null;
        String username = soloMias ? currentUser() : null;
        return conversationRepository.findAllByOrderByUltimoMensajeEnDescIdDesc().stream()
                .filter(item -> normalizedStatus == null || normalizedStatus.equals(item.getEstado()))
                .filter(item -> !soloNoLeidas || safeUnreadCount(item) > 0)
                .filter(item -> !soloMias || username.equals(item.getResponsableId()))
                .filter(item -> matchesConversation(item, normalizedQuery))
                .map(this::toConversationResponse)
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

    @Transactional
    public CrmWhatsappConversationResponse markConversationRead(Long prospectoId) {
        CrmWhatsappConversation conversation = requireConversation(prospectoId);
        OffsetDateTime readAt = OffsetDateTime.now(ZoneOffset.UTC);
        List<CrmWhatsappMessage> unreadMessages = messageRepository
                .findAllByProspecto_IdAndDireccionAndLeidoEnIsNull(prospectoId, "ENTRANTE");
        unreadMessages.forEach(message -> message.setLeidoEn(readAt));
        messageRepository.saveAll(unreadMessages);
        conversation.setNoLeidos(0);
        CrmWhatsappConversation saved = conversationRepository.save(conversation);

        messageRepository.findFirstByProspecto_IdAndDireccionOrderByMensajeEnDescIdDesc(prospectoId, "ENTRANTE")
                .ifPresent(message -> {
                    try {
                        cloudApiClient.markAsRead(requireActiveConfig(), message.getMetaMessageId());
                    } catch (BusinessException ex) {
                        log.warn("No se pudo confirmar lectura en Meta para wamid={}: {}", message.getMetaMessageId(), ex.getCode());
                    }
                });
        return toConversationResponse(saved);
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

    @Transactional
    public CrmWhatsappMessageResponse sendMessage(Long prospectoId, SendWhatsappMessageRequest request) {
        CrmProspecto prospecto = requireProspecto(prospectoId);
        CrmCanalTokenConfig config = requireActiveConfig();
        String recipient = normalizePhone(prospecto.getTelefono());
        String body = request.mensaje().trim();
        SendResult sendResult = cloudApiClient.sendText(config, recipient, body, Boolean.TRUE.equals(request.previewUrl()));

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
        CrmWhatsappMessage saved = messageRepository.save(message);
        updateConversation(prospecto, saved, false);
        createWhatsappActivity(prospecto, body, saved.getMensajeEn(), false);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponse> listProspectQuotes(Long prospectoId) {
        requireProspecto(prospectoId);
        return CotizacionMapper.toResponses(cotizacionRepository.findAllByCrmProspectoId(prospectoId));
    }

    @Transactional
    public SendWhatsappQuoteResponse sendQuote(
            Long prospectoId,
            Long quoteId,
            SendWhatsappQuoteRequest request) {
        CrmProspecto prospecto = requireProspecto(prospectoId);
        CrmCanalTokenConfig config = requireActiveConfig();
        Cotizacion quote = cotizacionRepository.findByIdAndCrmProspectoId(quoteId, prospectoId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_COTIZACION_NO_ENCONTRADA",
                        "La cotizacion no pertenece a este prospecto"
                ));
        String recipient = normalizePhone(prospecto.getTelefono());
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
        SendResult sendResult = cloudApiClient.sendDocument(
                config,
                recipient,
                mediaId,
                pdf.fileName(),
                caption
        );

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
        CrmWhatsappMessage saved = messageRepository.save(message);
        updateConversation(prospecto, saved, false);
        createWhatsappActivity(prospecto, caption, saved.getMensajeEn(), false);

        CotizacionResponse updatedQuote = updateCotizacionEstadoUseCase.execute(
                quoteId,
                new UpdateCotizacionEstadoRequest("ENVIADA", "WHATSAPP", null, null, null)
        );
        return new SendWhatsappQuoteResponse(toResponse(saved), updatedQuote);
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

        String sender = normalizePhone(text(messageNode, "from"));
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
        createWhatsappActivity(prospecto, body, messageTime, true);
        counters.processed++;
    }

    private void processStatus(JsonNode statusNode, Counters counters) {
        String metaMessageId = text(statusNode, "id");
        if (!hasText(metaMessageId)) {
            return;
        }
        messageRepository.findByMetaMessageId(metaMessageId).ifPresent(message -> {
            message.setEstado(normalizeStatus(text(statusNode, "status")));
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
            prospecto.setTipoPersona("NATURAL");
            prospecto.setNombre(truncate(firstNonBlank(contactName, sender), 180));
            prospecto.setTelefono(sender);
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
                                        boolean inbound) {
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
        activity.setUsuarioId(inbound ? PUBLIC_WHATSAPP_OWNER : currentUser());
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
        CrmProspecto prospecto = conversation.getProspecto();
        List<CrmWhatsappInternalNoteResponse> notes = conversationNoteRepository
                .findAllByConversation_IdOrderBySlotAsc(conversation.getId())
                .stream()
                .map(note -> new CrmWhatsappInternalNoteResponse(
                        note.getId(),
                        note.getSlot(),
                        note.getContenido(),
                        note.getCreatedAt(),
                        note.getUpdatedAt()
                ))
                .toList();
        String legacyNote = notes.isEmpty() ? conversation.getNotaInterna() : notes.getFirst().contenido();
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
                message.getCreatedAt()
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

    private String normalizePhone(String value) {
        String normalized = digits(value);
        if (!hasText(normalized) || normalized.length() < 8 || normalized.length() > 20) {
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

    private static final class Counters {
        private int processed;
        private int duplicates;
        private int statuses;
    }
}
