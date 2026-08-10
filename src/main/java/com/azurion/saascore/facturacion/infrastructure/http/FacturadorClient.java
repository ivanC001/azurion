package com.azurion.saascore.facturacion.infrastructure.http;

import com.azurion.saascore.facturacion.infrastructure.config.FacturadorProperties;
import com.azurion.saascore.facturacion.infrastructure.config.FacturadorProperties.FacturadorCredential;
import com.azurion.saascore.facturacion.infrastructure.security.FacturadorHmacSigner;
import com.azurion.saascore.ventas.application.dto.FormatoImpresionComprobante;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FacturadorClient {

    private static final long MIN_WAIT_PROCESSED_TIMEOUT_MS = 5_000;
    private static final long MAX_WAIT_PROCESSED_TIMEOUT_MS = 300_000;
    private static final long MIN_WAIT_PROCESSED_POLL_INTERVAL_MS = 250;
    private static final long MAX_WAIT_PROCESSED_POLL_INTERVAL_MS = 5_000;
    private static final long MAX_LIST_STATUS_TIMEOUT_MS = 4500;
    private static final int MAX_ARTIFACT_BYTES = 20 * 1024 * 1024;

    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_SIGNATURE_VERSION = "X-Signature-Version";

    private static final Set<String> TERMINAL_STATES = Set.of("ACEPTADO", "RECHAZADO", "ERROR");

    private final FacturadorProperties properties;
    private final ObjectMapper objectMapper;
    private final FacturadorHmacSigner hmacSigner;
    private final HttpClient httpClient;

    public FacturadorClient(
            FacturadorProperties properties,
            ObjectMapper objectMapper,
            FacturadorHmacSigner hmacSigner
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.hmacSigner = hmacSigner;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, properties.getConnectTimeoutMillis())))
                .build();
    }

    public FacturadorTenantProvisioningResult provisionarTenant(
            String externalTenantId,
            String businessName,
            String countryCode,
            String taxId,
            boolean active
    ) {
        FacturadorCredential credential = properties.resolveCredential(null)
                .orElseThrow(() -> new BusinessException(
                        "FACTURADOR_API_KEY_MISSING",
                        "No existe la credencial de integracion con el facturador"
                ));
        String safeTenantId = externalTenantId == null ? "" : externalTenantId.trim();
        if (!safeTenantId.matches("[A-Za-z0-9._:-]{2,80}")) {
            throw new BusinessException("FACTURADOR_TENANT_INVALID", "El identificador del tenant no es valido");
        }

        String path = normalizePath("/integrations/azurion/tenants/")
                + URLEncoder.encode(safeTenantId, StandardCharsets.UTF_8);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("business_name", businessName);
            payload.put("country_code", countryCode);
            payload.put("tax_id", taxId);
            payload.put("active", active);
            String body = objectMapper.writeValueAsString(payload);
            SignedCallResult response = executeSignedRequest("PUT", path, body, credential, null, safeTenantId);

            if (!isSuccessfulResponse(response.status(), response.body())) {
                throw new BusinessException("FACTURADOR_PROVISION_ERROR", resolveMessage(response.body(), response.status()));
            }

            JsonNode data = extractData(response.body());
            return new FacturadorTenantProvisioningResult(
                    response.status(),
                    text(data, "document_mode"),
                    text(data, "fiscal_status"),
                    text(data, "sunat_mode"),
                    response.body()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    "FACTURADOR_PROVISION_ERROR",
                    "No se pudo aprovisionar el tenant en el facturador: " + exception.getMessage()
            );
        }
    }

    public JsonNode listarTenantsAdministrados() {
        return executeManagementRequest(
                "GET",
                "/integrations/azurion/management/tenants",
                Map.of(),
                "platform",
                false
        );
    }

    public JsonNode obtenerTenantAdministrado(long tenantId) {
        return executeManagementRequest(
                "GET",
                "/integrations/azurion/management/tenants/" + tenantId,
                Map.of(),
                "platform",
                true
        );
    }

    public JsonNode obtenerTenantAdministradoPorExternalId(String externalTenantId) {
        String safeTenantId = requireSafeExternalTenantId(externalTenantId);
        return executeManagementRequest(
                "GET",
                "/integrations/azurion/management/tenants/external/"
                        + URLEncoder.encode(safeTenantId, StandardCharsets.UTF_8),
                Map.of(),
                safeTenantId,
                true
        );
    }

    public JsonNode crearTenantAdministrado(Map<String, Object> payload) {
        return executeManagementRequest(
                "POST",
                "/integrations/azurion/management/tenants",
                payload,
                "platform",
                false
        );
    }

    public JsonNode actualizarTenantAdministrado(long tenantId, Map<String, Object> payload) {
        return executeManagementRequest(
                "PUT",
                "/integrations/azurion/management/tenants/" + tenantId,
                payload,
                "platform",
                false
        );
    }

    public JsonNode actualizarTenantAdministradoPorExternalId(
            String externalTenantId,
            Map<String, Object> payload
    ) {
        String safeTenantId = requireSafeExternalTenantId(externalTenantId);
        return executeManagementRequest(
                "PUT",
                "/integrations/azurion/management/tenants/external/"
                        + URLEncoder.encode(safeTenantId, StandardCharsets.UTF_8),
                payload,
                safeTenantId,
                false
        );
    }

    private JsonNode executeManagementRequest(
            String method,
            String endpointPath,
            Map<String, Object> payload,
            String signedTenantId,
            boolean allowNotFound
    ) {
        FacturadorCredential credential = properties.resolveCredential(null)
                .orElseThrow(() -> new BusinessException(
                        "FACTURADOR_API_KEY_MISSING",
                        "No existe la credencial de integracion con el facturador"
                ));
        String requestPath = normalizePath(endpointPath);

        try {
            String body = "GET".equalsIgnoreCase(method)
                    ? ""
                    : objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
            SignedCallResult response = executeSignedRequest(
                    method,
                    requestPath,
                    body,
                    credential,
                    null,
                    signedTenantId
            );
            if (allowNotFound && response.status() == 404) {
                return null;
            }
            if (!isSuccessfulResponse(response.status(), response.body())) {
                throw new BusinessException(
                        "FACTURADOR_MANAGEMENT_ERROR",
                        resolveMessage(response.body(), response.status())
                );
            }
            return extractData(response.body());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    "FACTURADOR_MANAGEMENT_ERROR",
                    "No se pudo comunicar con el facturador: " + exception.getMessage()
            );
        }
    }

    private String requireSafeExternalTenantId(String externalTenantId) {
        String safeTenantId = externalTenantId == null ? "" : externalTenantId.trim();
        if (!safeTenantId.matches("[A-Za-z0-9._:-]{2,80}")) {
            throw new BusinessException("FACTURADOR_TENANT_INVALID", "El identificador del tenant no es valido");
        }
        return safeTenantId;
    }

    public FacturadorEmissionResult emitirDocumento(String tenantId, String tenantRuc, String endpointPath, Object payload, String tipoComprobante) {
        FacturadorCredential credential = properties.resolveCredential(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "FACTURADOR_API_KEY_MISSING",
                        "No existe API key configurada para facturador en el tenant " + tenantId
                ));

        String path = normalizePath(endpointPath);

        try {
            String body = objectMapper.writeValueAsString(payload);
            SignedCallResult initial = executeSignedRequest("POST", path, body, credential, tenantRuc, tenantId);

            if (!isSuccessfulResponse(initial.status(), initial.body())) {
                throw new BusinessException("FACTURADOR_ERROR", resolveMessage(initial.body(), initial.status()));
            }

            JsonNode finalBody = initial.body();
            int finalStatus = initial.status();
            String finalMessage = resolveMessage(finalBody, finalStatus);

            if (shouldWaitForProcessed(tipoComprobante, finalBody)) {
                SignedCallResult processed = waitForProcessed(credential, tenantRuc, tenantId, finalBody);
                if (processed != null) {
                    finalBody = processed.body();
                    finalStatus = processed.status();
                    finalMessage = resolveProcessedMessage(processed.body(), processed.status());
                } else {
                    finalMessage = finalMessage + " (pendiente de procesamiento SUNAT)";
                }
            }

            return new FacturadorEmissionResult(
                    true,
                    finalStatus,
                    path,
                    tipoComprobante,
                    finalMessage,
                    finalBody
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("FACTURADOR_ERROR", "No se pudo conectar con facturador: " + ex.getMessage());
        }
    }

    public Map<String, FacturadorDocumentoStatusResult> consultarDocumentosPorExternalIds(
            String tenantId,
            String tenantRuc,
            List<String> externalIds
    ) {
        List<String> normalizedIds = normalizeExternalIds(externalIds);
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }

        FacturadorCredential credential = properties.resolveCredential(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "FACTURADOR_API_KEY_MISSING",
                        "No existe API key configurada para facturador en el tenant " + tenantId
                ));

        Map<String, FacturadorDocumentoStatusResult> results = new LinkedHashMap<>();
        int chunkSize = 50;

        for (int start = 0; start < normalizedIds.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, normalizedIds.size());
            List<String> chunk = normalizedIds.subList(start, end);

            String csv = String.join(",", chunk);
            String requestPath = normalizePath("/documentos")
                    + "?limit=" + chunk.size()
                    + "&external_ids=" + encodeQueryValue(csv);

            try {
                long timeoutMs = Math.min(properties.getReadTimeoutMillis(), MAX_LIST_STATUS_TIMEOUT_MS);
                SignedCallResult response = executeSignedRequest("GET", requestPath, "", credential, tenantRuc, tenantId, timeoutMs);

                if (!isSuccessfulResponse(response.status(), response.body())) {
                    throw new BusinessException("FACTURADOR_STATUS_ERROR", resolveMessage(response.body(), response.status()));
                }

                mergeStatusItems(results, response);
            } catch (BusinessException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new BusinessException("FACTURADOR_STATUS_ERROR", "No se pudo consultar estados en facturador: " + exception.getMessage());
            }
        }

        return results;
    }

    /**
     * Obtiene una URL firmada nueva y descarga el PDF desde el facturador. La URL
     * temporal nunca se expone al navegador, por lo que no puede quedar obsoleta
     * en el estado de Angular ni reutilizarse para consultar otro tenant.
     */
    public FacturadorArtifactDownload descargarPdfComprobante(
            String tenantId,
            String tenantRuc,
            String externalId,
            FormatoImpresionComprobante formato
    ) {
        String safeExternalId = externalId == null ? "" : externalId.trim();
        if (safeExternalId.isBlank()) {
            throw new BusinessException("VENTA_EXTERNAL_ID_REQUIRED", "La venta no tiene identificador para facturacion");
        }

        FacturadorDocumentoStatusResult documento = consultarDocumentosPorExternalIds(
                tenantId,
                tenantRuc,
                List.of(safeExternalId)
        ).get(safeExternalId);

        if (documento == null) {
            throw BusinessException.notFound(
                    "FACTURADOR_DOCUMENT_NOT_FOUND",
                    "El comprobante no existe en el facturador"
            );
        }
        String selectedPdfUrl = switch (formato) {
            case A4 -> firstNonBlank(documento.pdfA4Url(), documento.pdfUrl());
            case TICKET -> firstNonBlank(documento.pdfTicketUrl(), documento.pdfUrl());
        };
        if (selectedPdfUrl == null || selectedPdfUrl.isBlank()) {
            throw BusinessException.conflict(
                    "FACTURADOR_PDF_NOT_READY",
                    "El PDF aun no esta disponible. Espera a que termine el procesamiento del comprobante"
            );
        }

        URI artifactUri = validateArtifactUri(selectedPdfUrl, documento.documentoId(), "pdf");
        try {
            HttpRequest request = HttpRequest.newBuilder(artifactUri)
                    .timeout(Duration.ofMillis(Math.max(500, properties.getReadTimeoutMillis())))
                    .header("Accept", "application/pdf")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] content = response.body() == null ? new byte[0] : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        "FACTURADOR_PDF_DOWNLOAD_ERROR",
                        response.statusCode() == 403
                                ? "La firma temporal del PDF fue rechazada por el facturador"
                                : "El facturador no pudo entregar el PDF (HTTP " + response.statusCode() + ")",
                        org.springframework.http.HttpStatus.BAD_GATEWAY
                );
            }
            if (content.length == 0 || content.length > MAX_ARTIFACT_BYTES || !hasPdfSignature(content)) {
                throw new BusinessException(
                        "FACTURADOR_PDF_INVALID",
                        "El facturador respondio con un archivo PDF invalido",
                        org.springframework.http.HttpStatus.BAD_GATEWAY
                );
            }

            String filename = safePdfFilename(tenantRuc, documento, formato);
            return new FacturadorArtifactDownload(content, filename, "application/pdf");
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    "FACTURADOR_PDF_DOWNLOAD_INTERRUPTED",
                    "La descarga del PDF fue interrumpida",
                    org.springframework.http.HttpStatus.BAD_GATEWAY
            );
        } catch (Exception exception) {
            throw new BusinessException(
                    "FACTURADOR_PDF_DOWNLOAD_ERROR",
                    "No se pudo descargar el PDF desde el facturador: " + exception.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY
            );
        }
    }

    public FacturadorArtifactDownload descargarXmlComprobante(
            String tenantId,
            String tenantRuc,
            String externalId
    ) {
        FacturadorDocumentoStatusResult documento = requireDocumentoFacturador(tenantId, tenantRuc, externalId);
        if (isTicket(documento)) {
            throw BusinessException.conflict(
                    "FACTURADOR_XML_NOT_APPLICABLE",
                    "Los tickets de venta no generan XML tributario"
            );
        }
        return descargarArchivoComprobante(
                documento.xmlUrl(),
                documento,
                "xml",
                "application/xml",
                safeArtifactFilename(tenantRuc, documento, ".xml")
        );
    }

    public FacturadorArtifactDownload descargarCdrComprobante(
            String tenantId,
            String tenantRuc,
            String externalId
    ) {
        FacturadorDocumentoStatusResult documento = requireDocumentoFacturador(tenantId, tenantRuc, externalId);
        if (isTicket(documento)) {
            throw BusinessException.conflict(
                    "FACTURADOR_CDR_NOT_APPLICABLE",
                    "Los tickets de venta no generan CDR de SUNAT"
            );
        }
        return descargarArchivoComprobante(
                documento.cdrUrl(),
                documento,
                "cdr",
                "application/zip",
                "R-" + safeArtifactFilename(tenantRuc, documento, ".zip")
        );
    }

    private FacturadorDocumentoStatusResult requireDocumentoFacturador(
            String tenantId,
            String tenantRuc,
            String externalId
    ) {
        String safeExternalId = externalId == null ? "" : externalId.trim();
        if (safeExternalId.isBlank()) {
            throw new BusinessException("VENTA_EXTERNAL_ID_REQUIRED", "La venta no tiene identificador para facturacion");
        }
        FacturadorDocumentoStatusResult documento = consultarDocumentosPorExternalIds(
                tenantId,
                tenantRuc,
                List.of(safeExternalId)
        ).get(safeExternalId);
        if (documento == null) {
            throw BusinessException.notFound(
                    "FACTURADOR_DOCUMENT_NOT_FOUND",
                    "El comprobante no existe en el facturador"
            );
        }
        return documento;
    }

    private FacturadorArtifactDownload descargarArchivoComprobante(
            String rawUrl,
            FacturadorDocumentoStatusResult documento,
            String artifact,
            String contentType,
            String filename
    ) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw BusinessException.conflict(
                    "FACTURADOR_ARTIFACT_NOT_READY",
                    "El archivo " + artifact.toUpperCase(Locale.ROOT) + " aun no esta disponible"
            );
        }

        URI artifactUri = validateArtifactUri(rawUrl, documento.documentoId(), artifact);
        try {
            HttpRequest request = HttpRequest.newBuilder(artifactUri)
                    .timeout(Duration.ofMillis(Math.max(500, properties.getReadTimeoutMillis())))
                    .header("Accept", contentType)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] content = response.body() == null ? new byte[0] : response.body();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        "FACTURADOR_ARTIFACT_DOWNLOAD_ERROR",
                        response.statusCode() == 403
                                ? "La firma temporal del archivo fue rechazada por el facturador"
                                : "El facturador no pudo entregar el archivo (HTTP " + response.statusCode() + ")",
                        org.springframework.http.HttpStatus.BAD_GATEWAY
                );
            }
            if (content.length == 0 || content.length > MAX_ARTIFACT_BYTES || !hasExpectedSignature(content, artifact)) {
                throw new BusinessException(
                        "FACTURADOR_ARTIFACT_INVALID",
                        "El facturador respondio con un archivo " + artifact.toUpperCase(Locale.ROOT) + " invalido",
                        org.springframework.http.HttpStatus.BAD_GATEWAY
                );
            }
            return new FacturadorArtifactDownload(content, filename, contentType);
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    "FACTURADOR_ARTIFACT_DOWNLOAD_INTERRUPTED",
                    "La descarga del archivo fue interrumpida",
                    org.springframework.http.HttpStatus.BAD_GATEWAY
            );
        } catch (Exception exception) {
            throw new BusinessException(
                    "FACTURADOR_ARTIFACT_DOWNLOAD_ERROR",
                    "No se pudo descargar el archivo desde el facturador: " + exception.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY
            );
        }
    }

    private URI validateArtifactUri(String rawUrl, Integer documentoId, String artifact) {
        try {
            URI candidate = URI.create(rawUrl.trim());
            String artifactBaseUrl = properties.getArtifactBaseUrl();
            String configuredArtifactBase = artifactBaseUrl == null || artifactBaseUrl.isBlank()
                    ? properties.getBaseUrl()
                    : artifactBaseUrl;
            URI configuredBase = URI.create(configuredArtifactBase.replaceAll("/+$", ""));
            String expectedPath = normalizePath("/documentos/")
                    + documentoId
                    + "/"
                    + artifact;
            boolean sameOrigin = configuredBase.getScheme().equalsIgnoreCase(candidate.getScheme())
                    && configuredBase.getHost().equalsIgnoreCase(candidate.getHost())
                    && effectivePort(configuredBase) == effectivePort(candidate);
            String query = candidate.getRawQuery();

            if (!sameOrigin
                    || !expectedPath.equals(candidate.getPath())
                    || query == null
                    || !query.matches("(^|.*&)signature=[A-Fa-f0-9]{32,}(&.*|$)")) {
                throw new IllegalArgumentException("URL de artefacto fuera del facturador configurado");
            }
            return candidate;
        } catch (Exception exception) {
            throw BusinessException.internal(
                    "FACTURADOR_ARTIFACT_URL_INVALID",
                    "El facturador devolvio una URL de artefacto no valida"
            );
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private boolean hasPdfSignature(byte[] content) {
        return content.length >= 5
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-';
    }

    private boolean hasExpectedSignature(byte[] content, String artifact) {
        if ("cdr".equals(artifact)) {
            return content.length >= 4 && content[0] == 'P' && content[1] == 'K';
        }
        if ("xml".equals(artifact)) {
            int index = 0;
            if (content.length >= 3
                    && (content[0] & 0xff) == 0xef
                    && (content[1] & 0xff) == 0xbb
                    && (content[2] & 0xff) == 0xbf) {
                index = 3;
            }
            while (index < content.length && Character.isWhitespace((char) content[index])) {
                index++;
            }
            return index < content.length && content[index] == '<';
        }
        return false;
    }

    private boolean isTicket(FacturadorDocumentoStatusResult documento) {
        String tipo = documento.tipoDocumento() == null ? "" : documento.tipoDocumento().trim();
        return "TK".equalsIgnoreCase(tipo) || "TICKET".equalsIgnoreCase(tipo);
    }

    private String safeArtifactFilename(
            String tenantRuc,
            FacturadorDocumentoStatusResult documento,
            String extension
    ) {
        String raw = String.join(
                "-",
                tenantRuc == null ? "comprobante" : tenantRuc,
                documento.tipoDocumento() == null ? "documento" : documento.tipoDocumento(),
                documento.serie() == null ? "sin-serie" : documento.serie(),
                documento.correlativo() == null ? String.valueOf(documento.documentoId()) : documento.correlativo()
        );
        return raw.replaceAll("[^A-Za-z0-9._-]", "_") + extension;
    }

    private String safePdfFilename(
            String tenantRuc,
            FacturadorDocumentoStatusResult documento,
            FormatoImpresionComprobante formato
    ) {
        String raw = String.join(
                "-",
                tenantRuc == null ? "comprobante" : tenantRuc,
                documento.tipoDocumento() == null ? "documento" : documento.tipoDocumento(),
                documento.serie() == null ? "sin-serie" : documento.serie(),
                documento.correlativo() == null ? String.valueOf(documento.documentoId()) : documento.correlativo()
        );
        return raw.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + formato.filenameSuffix() + ".pdf";
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private SignedCallResult waitForProcessed(
            FacturadorCredential credential,
            String tenantRuc,
            String tenantId,
            JsonNode initialBody
    ) throws Exception {
        Long documentoId = extractDocumentoId(initialBody);
        if (documentoId == null) {
            return null;
        }

        String statusPath = normalizePath("/sunat/estado") + "?documento_id=" + documentoId;
        long timeoutMs = Math.max(MIN_WAIT_PROCESSED_TIMEOUT_MS, properties.getWaitProcessedTimeoutMillis());
        timeoutMs = Math.min(timeoutMs, MAX_WAIT_PROCESSED_TIMEOUT_MS);

        long pollInterval = Math.max(MIN_WAIT_PROCESSED_POLL_INTERVAL_MS, properties.getWaitProcessedPollIntervalMillis());
        pollInterval = Math.min(pollInterval, MAX_WAIT_PROCESSED_POLL_INTERVAL_MS);
        if (pollInterval >= timeoutMs) {
            pollInterval = Math.max(MIN_WAIT_PROCESSED_POLL_INTERVAL_MS, timeoutMs / 4);
        }
        long deadlineEpochMillis = System.currentTimeMillis() + timeoutMs;

        SignedCallResult lastSuccessful = null;

        while (System.currentTimeMillis() <= deadlineEpochMillis) {
            SignedCallResult statusCall = executeSignedRequest("GET", statusPath, "", credential, tenantRuc, tenantId);
            if (!isSuccessfulResponse(statusCall.status(), statusCall.body())) {
                throw new BusinessException("FACTURADOR_STATUS_ERROR", resolveMessage(statusCall.body(), statusCall.status()));
            }

            lastSuccessful = statusCall;
            String estado = extractEstado(statusCall.body());

            if (TERMINAL_STATES.contains(estado)) {
                return statusCall;
            }

            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new BusinessException("FACTURADOR_WAIT_INTERRUPTED", "La espera de procesamiento SUNAT fue interrumpida.");
            }
        }

        return lastSuccessful;
    }

    private boolean shouldWaitForProcessed(String tipoComprobante, JsonNode initialBody) {
        if (!properties.isWaitProcessedEnabled()) {
            return false;
        }

        String tipo = tipoComprobante == null ? "" : tipoComprobante.trim().toUpperCase(Locale.ROOT);
        if ("TICKET_VENTA".equals(tipo)
                || "GUIA_REMISION".equals(tipo)
                || "NOTA_CREDITO".equals(tipo)
                || "NOTA_DEBITO".equals(tipo)
                || "07".equals(tipo)
                || "08".equals(tipo)
                || "09".equals(tipo)) {
            return false;
        }

        Long documentoId = extractDocumentoId(initialBody);
        if (documentoId == null) {
            return false;
        }

        JsonNode data = extractData(initialBody);
        if (data != null && data.has("sunat_async") && !data.path("sunat_async").asBoolean(true)) {
            return false;
        }

        return true;
    }

    private List<String> normalizeExternalIds(List<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String externalId : externalIds) {
            if (externalId == null) {
                continue;
            }
            String trimmed = externalId.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
            if (normalized.size() >= 200) {
                break;
            }
        }
        return normalized;
    }

    private void mergeStatusItems(Map<String, FacturadorDocumentoStatusResult> target, SignedCallResult response) {
        JsonNode body = response.body();
        JsonNode data = extractData(body);
        if (data == null || data.isNull() || data.isMissingNode()) {
            return;
        }

        JsonNode items = data.path("items");
        if (items == null || items.isNull() || items.isMissingNode()) {
            return;
        }
        if (!items.isArray()) {
            return;
        }

        for (JsonNode item : items) {
            String externalId = text(item, "external_id");
            if (externalId == null || externalId.isBlank()) {
                continue;
            }

            FacturadorDocumentoStatusResult result = new FacturadorDocumentoStatusResult(
                    externalId,
                    integer(item, "id"),
                    text(item, "tipo_documento"),
                    text(item, "serie"),
                    text(item, "correlativo"),
                    text(item, "estado"),
                    text(item, "sunat_estado"),
                    text(item, "sunat_mensaje"),
                    text(item, "sunat_codigo_error"),
                    text(item, "ticket"),
                    text(item, "hash"),
                    text(item, "pdf_url"),
                    text(item, "pdf_a4_url"),
                    text(item, "pdf_ticket_url"),
                    text(item, "xml_url"),
                    text(item, "cdr_url"),
                    response.status(),
                    item
            );
            target.put(externalId, result);
        }
    }

    private String encodeQueryValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode source, String fieldName) {
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }
        JsonNode node = source.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText("").trim();
        return value.isBlank() ? null : value;
    }

    private Integer integer(JsonNode source, String fieldName) {
        if (source == null || source.isNull() || source.isMissingNode()) {
            return null;
        }
        JsonNode node = source.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.canConvertToInt()) {
            return node.asInt();
        }
        return null;
    }

    private SignedCallResult executeSignedRequest(
            String method,
            String requestUri,
            String requestBody,
            FacturadorCredential credential,
            String tenantRuc
    ) throws Exception {
        return executeSignedRequest(method, requestUri, requestBody, credential, tenantRuc, null);
    }

    private SignedCallResult executeSignedRequest(
            String method,
            String requestUri,
            String requestBody,
            FacturadorCredential credential,
            String tenantRuc,
            String externalTenantId
    ) throws Exception {
        return executeSignedRequest(
                method,
                requestUri,
                requestBody,
                credential,
                tenantRuc,
                externalTenantId,
                properties.getReadTimeoutMillis()
        );
    }

    private SignedCallResult executeSignedRequest(
            String method,
            String requestUri,
            String requestBody,
            FacturadorCredential credential,
            String tenantRuc,
            String externalTenantId,
            long requestTimeoutMs
    ) throws Exception {
        String normalizedMethod = method == null ? "GET" : method.trim().toUpperCase(Locale.ROOT);
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        URI endpoint = URI.create(baseUrl + requestUri);

        String body = requestBody == null ? "" : requestBody;
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String signedTenantId = externalTenantId == null || externalTenantId.isBlank()
                ? ""
                : trimHeaderValue(externalTenantId, 80);
        String signedTenantRuc = tenantRuc == null || tenantRuc.isBlank()
                ? ""
                : trimHeaderValue(tenantRuc, 40);
        String signature = hmacSigner.sign(
                credential.signatureVersion(),
                normalizedMethod,
                requestUri,
                timestamp,
                nonce,
                signedTenantId,
                signedTenantRuc,
                body,
                credential.secret()
        );

        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(Math.max(500, requestTimeoutMs)))
                .header("Accept", "application/json")
                .header(HEADER_TIMESTAMP, timestamp)
                .header(HEADER_NONCE, nonce)
                .header(HEADER_SIGNATURE, signature)
                .header(HEADER_SIGNATURE_VERSION, credential.signatureVersion());

        if (credential.legacyApiKey()) {
            builder.header(HEADER_API_KEY, credential.secret());
        } else {
            builder.header(HEADER_CLIENT_ID, trimHeaderValue(credential.clientId(), 120));
        }

        if (!signedTenantRuc.isBlank()) {
            builder.header("X-Tenant-RUC", signedTenantRuc);
        }
        if (!signedTenantId.isBlank()) {
            builder.header("X-Azurion-Tenant-ID", signedTenantId);
        }

        if ("POST".equals(normalizedMethod) || "PUT".equals(normalizedMethod)) {
            String idempotencyKey = extractIdempotencyKey(body);
            builder.header("Content-Type", "application/json");
            if (idempotencyKey != null) {
                builder.header("Idempotency-Key", idempotencyKey);
            }
            builder.method(normalizedMethod, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new SignedCallResult(response.statusCode(), parseBody(response.body()));
    }

    private String extractIdempotencyKey(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode payload = objectMapper.readTree(body);
            String externalId = readExternalId(payload);
            if (externalId.isBlank() && payload.has("documento")) {
                externalId = readExternalId(payload.path("documento"));
            }
            return externalId.isBlank() ? null : trimHeaderValue(externalId, 180);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readExternalId(JsonNode source) {
        String externalId = source.path("external_id").asText("").trim();
        return externalId.isBlank()
                ? source.path("externalId").asText("").trim()
                : externalId;
    }

    private String trimHeaderValue(String value, int maxLength) {
        String safe = value.replace("\r", "").replace("\n", "").trim();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private String normalizePath(String endpointPath) {
        String prefix = properties.getApiPrefix() == null ? "/api" : properties.getApiPrefix().trim();
        if (prefix.isBlank()) {
            prefix = "/api";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        prefix = prefix.replaceAll("/+$", "");

        String path = endpointPath == null ? "" : endpointPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (path.startsWith(prefix + "/") || path.equals(prefix)) {
            return path;
        }
        return prefix + path;
    }

    private JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSuccessfulResponse(int status, JsonNode json) {
        return status >= 200 && status < 300 && (json == null || json.path("success").asBoolean(true));
    }

    private String resolveMessage(JsonNode json, int status) {
        if (json == null) {
            return "Facturador respondio sin contenido (" + status + ")";
        }

        String validationMessage = firstValidationMessage(json.path("errors"));
        if (!validationMessage.isBlank()) {
            return validationMessage;
        }

        String backendMessage = json.path("message").asText("");
        if (!backendMessage.isBlank()) {
            return backendMessage;
        }
        if (json.path("success").asBoolean(false)) {
            return "Documento enviado al facturador";
        }
        return "Facturador rechazo la solicitud (" + status + ")";
    }

    private String firstValidationMessage(JsonNode errors) {
        if (errors == null || !errors.isObject()) {
            return "";
        }

        var fields = errors.fields();
        if (!fields.hasNext()) {
            return "";
        }

        var field = fields.next();
        JsonNode messages = field.getValue();
        String message = messages.isArray() && !messages.isEmpty()
                ? messages.get(0).asText("")
                : messages.asText("");
        if (message.isBlank()) {
            return "";
        }
        return "El dato " + field.getKey() + " no es valido: " + message;
    }

    private String resolveProcessedMessage(JsonNode json, int status) {
        JsonNode data = extractData(json);
        String estado = data == null ? "" : data.path("estado").asText("");
        if (!estado.isBlank()) {
            return "Documento procesado por SUNAT con estado: " + estado;
        }
        return resolveMessage(json, status);
    }

    private JsonNode extractData(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return null;
        }
        return data;
    }

    private Long extractDocumentoId(JsonNode root) {
        JsonNode data = extractData(root);
        if (data == null || !data.has("documento_id") || !data.path("documento_id").canConvertToLong()) {
            return null;
        }
        return data.path("documento_id").asLong();
    }

    private String extractEstado(JsonNode root) {
        JsonNode data = extractData(root);
        if (data == null) {
            return "";
        }
        return data.path("estado").asText("").trim().toUpperCase(Locale.ROOT);
    }

    private record SignedCallResult(int status, JsonNode body) {
    }

    public record FacturadorEmissionResult(
            boolean success,
            int status,
            String endpoint,
            String tipoComprobante,
            String message,
            JsonNode responseBody
    ) {
    }

    public record FacturadorDocumentoStatusResult(
            String externalId,
            Integer documentoId,
            String tipoDocumento,
            String serie,
            String correlativo,
            String estadoInterno,
            String sunatEstado,
            String sunatMensaje,
            String sunatCodigoError,
            String ticket,
            String hash,
            String pdfUrl,
            String pdfA4Url,
            String pdfTicketUrl,
            String xmlUrl,
            String cdrUrl,
            int httpStatus,
            JsonNode rawData
    ) {
    }

    public record FacturadorArtifactDownload(
            byte[] content,
            String filename,
            String contentType
    ) {
    }

    public record FacturadorTenantProvisioningResult(
            int httpStatus,
            String documentMode,
            String fiscalStatus,
            String sunatMode,
            JsonNode rawData
    ) {
    }
}
