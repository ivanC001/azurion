package com.azurion.saascore.crm.application.usecases;

import static com.azurion.saascore.crm.application.support.CrmSupport.firstNonBlank;
import static com.azurion.saascore.crm.application.support.CrmSupport.hasText;
import static com.azurion.saascore.crm.application.support.CrmSupport.normalizeSearch;
import static com.azurion.saascore.crm.application.support.CrmSupport.required;
import static com.azurion.saascore.crm.application.support.CrmSupport.requireEnum;
import static com.azurion.saascore.crm.application.support.CrmSupport.safePageable;
import static com.azurion.saascore.crm.application.support.CrmSupport.trim;
import static com.azurion.saascore.crm.application.support.CrmSupport.updateIfPresent;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.application.dto.CrmCanalTokenConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmCurrencyConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmCurrencyOptionResponse;
import com.azurion.saascore.crm.application.dto.CrmInboxChannelResponse;
import com.azurion.saascore.crm.application.dto.CrmSentEmailResponse;
import com.azurion.saascore.crm.application.dto.UpdateCrmCanalTokenConfigRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmCurrencyConfigRequest;
import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.shared.api.PageResponse;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.money.CurrencyCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuracion de canales de ingreso y monedas del CRM.
 *
 * Extraido de CrmUseCaseService: es la porcion mas independiente del modulo
 * (no toca prospectos, oportunidades ni actividades) y arrastraba consigo doce
 * helpers privados que solo usaba ella.
 */
@Service
@RequiredArgsConstructor
public class CrmConfiguracionUseCase {

    private static final Set<String> CANALES = Set.of("WEB", "WHATSAPP", "INSTAGRAM", "FACEBOOK");
    private static final List<String> CANALES_ORDENADOS = List.of("WEB", "WHATSAPP", "INSTAGRAM", "FACEBOOK");
    private static final Set<String> CANALES_META = Set.of("FACEBOOK", "INSTAGRAM");
    private static final Set<String> HOSTS_NO_PUBLICOS = Set.of("localhost", "127.0.0.1", "::1");

    private final CrmCurrencyConfigRepository currencyConfigRepository;
    private final CrmCanalTokenConfigRepository canalTokenConfigRepository;
    private final CrmOportunidadRepository oportunidadRepository;
    private final CotizacionRepository cotizacionRepository;
    private final CrmSecretEncryptionService crmSecretEncryptionService;

    @Transactional(readOnly = true)
    public List<CrmCurrencyConfigResponse> listCurrencyConfig() {
        Map<String, CrmCurrencyConfig> existing = new LinkedHashMap<>();
        for (CrmCurrencyConfig item : currencyConfigRepository.findAllByOrderByMonedaAsc()) {
            existing.put(item.getMoneda(), item);
        }
        existing.putIfAbsent("USD", defaultCurrencyConfig("USD"));
        existing.putIfAbsent("EUR", defaultCurrencyConfig("EUR"));
        return existing.values().stream()
                .sorted(Comparator.comparing(CrmCurrencyConfig::getMoneda))
                .map(this::toCurrencyConfigResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmCurrencyOptionResponse> listAvailableCurrencies() {
        return Currency.getAvailableCurrencies().stream()
                .sorted(Comparator.comparing(Currency::getCurrencyCode))
                .map(currency -> new CrmCurrencyOptionResponse(
                        currency.getCurrencyCode(),
                        CurrencyCatalog.displayName(currency.getCurrencyCode()),
                        CurrencyCatalog.symbol(currency.getCurrencyCode())
                ))
                .toList();
    }

    @Transactional
    public CrmCurrencyConfigResponse saveCurrencyConfig(UpdateCrmCurrencyConfigRequest request) {
        String moneda = CurrencyCatalog.normalize(request.moneda(), "CRM_MONEDA_INVALIDA");
        CrmCurrencyConfig config = currencyConfigRepository.findByMoneda(moneda)
                .orElseGet(() -> defaultCurrencyConfig(moneda));
        config.setMoneda(moneda);
        updateIfPresent(request.nombre(), (value) -> config.setNombre(required(value, "El nombre de la moneda es obligatorio")));
        updateIfPresent(request.simbolo(), (value) -> config.setSimbolo(required(value, "El símbolo de la moneda es obligatorio")));
        if (request.tipoCambioBase() != null) {
            if (request.tipoCambioBase().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("CRM_TIPO_CAMBIO_INVALIDO", "El tipo de cambio debe ser mayor a cero");
            }
            config.setTipoCambioBase(request.tipoCambioBase().setScale(6, RoundingMode.HALF_UP));
        }
        if (request.margenConversionPorcentaje() != null) {
            if (request.margenConversionPorcentaje().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("CRM_MARGEN_CONVERSION_INVALIDO", "El margen de conversión no puede ser negativo");
            }
            config.setMargenConversionPorcentaje(request.margenConversionPorcentaje().setScale(4, RoundingMode.HALF_UP));
        }
        if (request.activo() != null) {
            config.setActivo(request.activo());
        }
        return toCurrencyConfigResponse(currencyConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<CrmCanalTokenConfigResponse> listCanalTokenConfig() {
        Map<String, CrmCanalTokenConfig> existing = canalesByCode();
        return CANALES_ORDENADOS.stream()
                .map((canal) -> toCanalTokenConfigResponse(existing.getOrDefault(canal, defaultCanalConfig(canal))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmInboxChannelResponse> listInboxChannels(boolean emailActive) {
        Map<String, CrmCanalTokenConfig> existing = canalesByCode();
        return List.of(
                inboxChannel("WHATSAPP", "WhatsApp", existing),
                inboxChannel("FACEBOOK", "Facebook", existing),
                inboxChannel("INSTAGRAM", "Instagram", existing),
                new CrmInboxChannelResponse("CORREO", "Correo", emailActive)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CrmSentEmailResponse> pageSentEmails(String query, int page, int size) {
        String normalizedQuery = normalizeSearch(query);
        Page<Cotizacion> result = cotizacionRepository.findSentEmails(
                normalizedQuery == null ? "" : normalizedQuery,
                safePageable(page, size, Sort.by(Sort.Order.desc("fechaEnvio"), Sort.Order.desc("id")))
        );
        return PageResponse.from(result, result.getContent().stream().map(this::toSentEmailResponse).toList());
    }

    @Transactional
    public CrmCanalTokenConfigResponse saveCanalTokenConfig(UpdateCrmCanalTokenConfigRequest request) {
        String canal = requireEnum(request.canal(), CANALES, "CRM_CANAL_INVALIDO");
        boolean whatsappConnectionChanged = "WHATSAPP".equals(canal) && (
                hasText(request.accessToken())
                        || hasText(request.appId())
                        || hasText(request.appSecret())
                        || hasText(request.phoneNumberId())
                        || hasText(request.wabaId())
        );
        boolean whatsappVerifyTokenChanged = "WHATSAPP".equals(canal) && hasText(request.verifyToken());
        boolean metaWebhookChanged = CANALES_META.contains(canal) && (
                hasText(request.verifyToken())
                        || hasText(request.webhookUrl())
                        || hasText(request.appId())
                        || hasText(request.appSecret())
        );
        CrmCanalTokenConfig config = canalTokenConfigRepository.findByCanal(canal)
                .orElseGet(() -> {
                    CrmCanalTokenConfig item = new CrmCanalTokenConfig();
                    item.setCanal(canal);
                    item.setNombre(defaultCanalName(canal));
                    return item;
                });
        updateIfPresent(request.nombre(), value -> config.setNombre(trim(value)));
        updateIfPresent(request.accessToken(), value -> config.setAccessToken(crmSecretEncryptionService.encrypt(trim(value))));
        updateIfPresent(request.verifyToken(), value -> config.setVerifyToken(crmSecretEncryptionService.encrypt(trim(value))));
        updateIfPresent(request.webhookUrl(), value -> config.setWebhookUrl(trim(value)));
        updateIfPresent(request.appId(), value -> config.setAppId(trim(value)));
        updateIfPresent(request.appSecret(), value -> config.setAppSecret(crmSecretEncryptionService.encrypt(trim(value))));
        updateIfPresent(request.phoneNumberId(), value -> config.setPhoneNumberId(trim(value)));
        updateIfPresent(request.wabaId(), value -> config.setWabaId(trim(value)));
        updateIfPresent(request.metadataJson(), value -> config.setMetadataJson(trim(value)));
        if (request.activo() != null) {
            config.setActivo(request.activo());
        }
        if ("WHATSAPP".equals(canal)) {
            config.setWebhookUrl(null);
            config.setMetadataJson(null);
            if (whatsappConnectionChanged) {
                resetWhatsappConnectionStatus(config);
            }
            if (whatsappVerifyTokenChanged) {
                config.setWebhookVerifiedAt(null);
            }
        } else if (metaWebhookChanged) {
            config.setWebhookVerifiedAt(null);
        }
        validateWhatsappConfig(config);
        validateMetaWebhookConfig(config);
        return toCanalTokenConfigResponse(canalTokenConfigRepository.save(config));
    }

    private Map<String, CrmCanalTokenConfig> canalesByCode() {
        Map<String, CrmCanalTokenConfig> existing = new LinkedHashMap<>();
        for (CrmCanalTokenConfig item : canalTokenConfigRepository.findAllByOrderByCanalAsc()) {
            existing.put(item.getCanal(), item);
        }
        return existing;
    }

    private CrmCanalTokenConfig defaultCanalConfig(String canal) {
        CrmCanalTokenConfig config = new CrmCanalTokenConfig();
        config.setCanal(canal);
        config.setNombre(defaultCanalName(canal));
        config.setActivo(false);
        return config;
    }

    private CrmInboxChannelResponse inboxChannel(String canal,
                                                 String nombre,
                                                 Map<String, CrmCanalTokenConfig> existing) {
        CrmCanalTokenConfig config = existing.get(canal);
        return new CrmInboxChannelResponse(canal, nombre, config != null && config.isActivo());
    }

    private CrmCurrencyConfig defaultCurrencyConfig(String moneda) {
        String normalized = CurrencyCatalog.normalize(moneda, "CRM_MONEDA_INVALIDA");
        CrmCurrencyConfig config = new CrmCurrencyConfig();
        config.setMoneda(normalized);
        config.setNombre(CurrencyCatalog.displayName(normalized));
        config.setSimbolo(CurrencyCatalog.symbol(normalized));
        config.setTipoCambioBase(defaultExchangeRate(normalized));
        config.setMargenConversionPorcentaje(BigDecimal.ZERO);
        config.setActivo(false);
        return config;
    }

    private BigDecimal defaultExchangeRate(String currency) {
        return switch (currency) {
            case "USD" -> new BigDecimal("3.800000");
            case "EUR" -> new BigDecimal("4.100000");
            default -> BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
        };
    }

    private String defaultCanalName(String canal) {
        return switch (canal) {
            case "WEB" -> "Landing web";
            case "WHATSAPP" -> "WhatsApp Business";
            case "INSTAGRAM" -> "Instagram";
            case "FACEBOOK" -> "Facebook Lead Ads";
            default -> canal;
        };
    }

    private CrmCurrencyConfigResponse toCurrencyConfigResponse(CrmCurrencyConfig config) {
        BigDecimal base = config.getTipoCambioBase() == null ? BigDecimal.ONE : config.getTipoCambioBase();
        BigDecimal margin = config.getMargenConversionPorcentaje() == null ? BigDecimal.ZERO : config.getMargenConversionPorcentaje();
        BigDecimal saleRate = base
                .multiply(BigDecimal.ONE.add(margin.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)))
                .setScale(6, RoundingMode.HALF_UP);
        return new CrmCurrencyConfigResponse(
                config.getId(),
                config.getMoneda(),
                config.getNombre(),
                config.getSimbolo(),
                base.setScale(6, RoundingMode.HALF_UP),
                margin.setScale(4, RoundingMode.HALF_UP),
                saleRate,
                config.isActivo()
        );
    }

    /**
     * Los secretos nunca se devuelven: solo se informa si estan configurados.
     */
    private CrmCanalTokenConfigResponse toCanalTokenConfigResponse(CrmCanalTokenConfig config) {
        return new CrmCanalTokenConfigResponse(
                config.getId(),
                config.getCanal(),
                config.getNombre(),
                null,
                null,
                config.getWebhookUrl(),
                config.getAppId(),
                config.getPhoneNumberId(),
                config.getWabaId(),
                hasText(config.getAccessToken()),
                hasText(config.getVerifyToken()),
                hasText(config.getAppSecret()),
                config.getWebhookVerifiedAt(),
                config.getLastConnectionTestAt(),
                config.getLastConnectionOk(),
                config.getLastConnectionMessage(),
                config.getWabaSubscribed(),
                config.getMetaDisplayPhoneNumber(),
                config.getMetaVerifiedName(),
                config.getMetaQualityRating(),
                config.getMetaTokenExpiresAt(),
                config.isActivo(),
                config.getMetadataJson()
        );
    }

    private void validateWhatsappConfig(CrmCanalTokenConfig config) {
        if (!"WHATSAPP".equals(config.getCanal()) || !config.isActivo()) {
            return;
        }
        if (!hasText(config.getPhoneNumberId())) {
            throw new BusinessException("CRM_WHATSAPP_PHONE_ID_REQUERIDO", "Configura el Phone number ID de WhatsApp");
        }
        if (!hasText(config.getWabaId())) {
            throw new BusinessException("CRM_WHATSAPP_WABA_ID_REQUERIDO", "Configura el WABA ID de WhatsApp");
        }
        if (!hasText(config.getAccessToken())) {
            throw new BusinessException("CRM_WHATSAPP_ACCESS_TOKEN_REQUERIDO", "Configura el access token de WhatsApp");
        }
        if (!hasText(config.getVerifyToken())) {
            throw new BusinessException("CRM_WHATSAPP_VERIFY_TOKEN_REQUERIDO", "Configura el verify token del webhook");
        }
        if (!hasText(config.getAppSecret())) {
            throw new BusinessException("CRM_WHATSAPP_APP_SECRET_REQUERIDO", "Configura el App secret para validar la firma del webhook");
        }
        if (!hasText(config.getAppId())) {
            throw new BusinessException("CRM_WHATSAPP_APP_ID_REQUERIDO", "Configura el App ID de Meta");
        }
    }

    private void validateMetaWebhookConfig(CrmCanalTokenConfig config) {
        if (!CANALES_META.contains(config.getCanal()) || !config.isActivo()) {
            return;
        }
        String channelName = "FACEBOOK".equals(config.getCanal()) ? "Facebook" : "Instagram";
        if (!hasText(config.getAccessToken())) {
            throw new BusinessException("CRM_META_ACCESS_TOKEN_REQUERIDO", "Configura el access token de " + channelName);
        }
        if (!hasText(config.getVerifyToken())) {
            throw new BusinessException("CRM_META_VERIFY_TOKEN_REQUERIDO", "Configura el verify token del webhook de " + channelName);
        }
        if (!hasText(config.getAppId())) {
            throw new BusinessException("CRM_META_APP_ID_REQUERIDO", "Configura el App ID de Meta para " + channelName);
        }
        if (!hasText(config.getAppSecret())) {
            throw new BusinessException("CRM_META_APP_SECRET_REQUERIDO", "Configura el App secret de Meta para " + channelName);
        }
        if (!isPublicHttpsUrl(config.getWebhookUrl())) {
            throw new BusinessException(
                    "CRM_META_WEBHOOK_HTTPS_REQUERIDO",
                    "El webhook de " + channelName + " debe usar una URL publica HTTPS"
            );
        }
    }

    /**
     * Meta rechaza webhooks que no sean HTTPS publicos, asi que se valida antes
     * de guardar en lugar de fallar mas tarde contra su API.
     */
    private boolean isPublicHttpsUrl(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && hasText(host)
                    && !HOSTS_NO_PUBLICOS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private CrmSentEmailResponse toSentEmailResponse(Cotizacion quote) {
        Cliente recipient = quote.getCliente();
        String recipientName = recipient == null ? null : recipient.getNombre();
        String recipientEmail = recipient == null ? null : recipient.getEmail();
        if (quote.getCrmOportunidadId() != null && (!hasText(recipientName) || !hasText(recipientEmail))) {
            CrmOportunidad opportunity = oportunidadRepository.findById(quote.getCrmOportunidadId()).orElse(null);
            if (opportunity != null) {
                Cliente opportunityClient = opportunity.getCliente();
                CrmProspecto prospect = opportunity.getProspecto();
                recipientName = firstNonBlank(
                        recipientName,
                        opportunityClient == null ? null : opportunityClient.getNombre(),
                        prospect == null ? null : prospect.getNombre(),
                        "Destinatario CRM"
                );
                recipientEmail = firstNonBlank(
                        recipientEmail,
                        opportunityClient == null ? null : opportunityClient.getEmail(),
                        prospect == null ? null : prospect.getCorreo()
                );
            }
        }
        return new CrmSentEmailResponse(
                quote.getId(),
                quote.getCrmOportunidadId(),
                firstNonBlank(recipientName, "Destinatario CRM"),
                recipientEmail,
                "Cotizacion COT-" + String.format(Locale.ROOT, "%03d", quote.getId()),
                quote.getMoneda(),
                quote.getTotal(),
                quote.getEstado(),
                quote.getUsuarioNombre(),
                quote.getFechaEnvio()
        );
    }

    private void resetWhatsappConnectionStatus(CrmCanalTokenConfig config) {
        config.setLastConnectionTestAt(null);
        config.setLastConnectionOk(null);
        config.setLastConnectionMessage(null);
        config.setWabaSubscribed(null);
        config.setMetaDisplayPhoneNumber(null);
        config.setMetaVerifiedName(null);
        config.setMetaQualityRating(null);
        config.setMetaTokenExpiresAt(null);
    }
}
