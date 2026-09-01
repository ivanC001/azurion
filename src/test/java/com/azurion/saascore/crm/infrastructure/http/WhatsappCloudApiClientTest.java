package com.azurion.saascore.crm.infrastructure.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhatsappCloudApiClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private WhatsappCloudApiClient client;
    private CrmCanalTokenConfig config;
    private final List<String> paths = new ArrayList<>();
    private final List<String> auth = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private String response;
    private String secondPage;
    private int status = 200;

    @BeforeEach void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().toString());
            auth.add(exchange.getRequestHeaders().getFirst("Authorization"));
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = secondPage != null && exchange.getRequestURI().getQuery().contains("after=") ? secondPage : response;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        var secrets = mock(CrmSecretEncryptionService.class);
        when(secrets.decrypt("encrypted-a")).thenReturn("test-token-a");
        when(secrets.decrypt("encrypted-b")).thenReturn("test-token-b");
        client = new WhatsappCloudApiClient(mapper, secrets, "http://127.0.0.1:" + server.getAddress().getPort(), "v23.0", 1000, 2000);
        config = config("111", "222", "encrypted-a");
    }

    @AfterEach void close() { server.stop(0); }

    @Test void syncsAllPagesOnTheConfiguredHostAndOnlyApprovedStatuses() throws Exception {
        response = """
                {"data":[{"id":"1","name":"pending","status":"PENDING","language":"es_PE"}],
                 "paging":{"next":"https://untrusted.invalid/?access_token=do-not-follow","cursors":{"after":"next+page"}}}
                """;
        secondPage = catalog("APPROVED", "Hola {{1}}, tu solicitud sobre {{2}}.");
        var templates = client.listApprovedTemplates(config);
        assertEquals(1, templates.size());
        assertEquals("seguimiento_prospecto", templates.getFirst().name());
        assertEquals("es_PE", templates.getFirst().languageCode());
        assertEquals("t-1", templates.getFirst().id());
        assertEquals(2, templates.getFirst().parameterCount());
        assertTrue(paths.get(1).startsWith("/v23.0/111/message_templates?"));
        assertTrue(paths.get(1).endsWith("after=next%2Bpage"));
        assertEquals(List.of("Bearer test-token-a", "Bearer test-token-a"), auth);
        assertTrue(paths.stream().noneMatch(path -> path.contains("token")));
    }

    @Test void usesEachChannelsWabaAndTokenWithoutCrossTenantCache() {
        response = "{\"data\":[]}";
        client.listApprovedTemplates(config);
        client.listApprovedTemplates(config("333", "444", "encrypted-b"));
        assertTrue(paths.get(0).contains("/111/message_templates"));
        assertTrue(paths.get(1).contains("/333/message_templates"));
        assertEquals(List.of("Bearer test-token-a", "Bearer test-token-b"), auth);
    }

    @Test void sendsTheExactDynamicTemplatePayloadAndReturnsTheWamid() throws Exception {
        var template = WhatsappTemplateParser.approved(mapper.readTree(catalog("APPROVED", "Hola {{1}}, solicitud sobre {{2}}"))).getFirst();
        response = "{\"messages\":[{\"id\":\"wamid.test-only\"}],\"contacts\":[{\"wa_id\":\"51999888777\"}]}";
        var result = client.sendTemplate(config, "51999888777", template, List.of("Ivan Flores", "Curso Python"));
        JsonNode payload = mapper.readTree(bodies.getFirst());
        assertEquals("/v23.0/222/messages", paths.getFirst());
        assertEquals("template", payload.path("type").asText());
        assertEquals("51999888777", payload.path("to").asText());
        assertEquals("seguimiento_prospecto", payload.at("/template/name").asText());
        assertEquals("es_PE", payload.at("/template/language/code").asText());
        assertEquals("body", payload.at("/template/components/0/type").asText());
        assertEquals("Ivan Flores", payload.at("/template/components/0/parameters/0/text").asText());
        assertEquals("Curso Python", payload.at("/template/components/0/parameters/1/text").asText());
        assertFalse(payload.at("/template/components/0/parameters/0").has("parameter_name"));
        assertEquals("wamid.test-only", result.metaMessageId());
    }

    @Test void supportsNamedTextParametersAndHeaderComponents() throws Exception {
        response = "{\"messages\":[{\"id\":\"wamid.named\"}]}";
        var template = WhatsappTemplateParser.approved(mapper.readTree("""
                {"data":[{"id":"2","name":"named","language":"pt_BR","status":"APPROVED","components":[
                {"type":"BODY","text":"Ola {{nome}}"},{"type":"HEADER","format":"TEXT","text":"Pedido {{1}}"}]}]}
                """)).getFirst();
        client.sendTemplate(config, "551199998888", template, List.of("123", "Ana"));
        var payload = mapper.readTree(bodies.getFirst());
        assertEquals("header", payload.at("/template/components/0/type").asText());
        assertEquals("123", payload.at("/template/components/0/parameters/0/text").asText());
        assertEquals("nome", payload.at("/template/components/1/parameters/0/parameter_name").asText());
    }

    @Test void keepsUnsupportedApprovedTemplatesVisibleButNotSendable() throws Exception {
        var templates = WhatsappTemplateParser.approved(mapper.readTree("""
                {"data":[{"name":"media","language":"es_PE","status":"APPROVED","components":[
                {"type":"HEADER","format":"IMAGE"},{"type":"BODY","text":"Hola"}]}]}
                """));
        assertFalse(templates.getFirst().available());
        assertNotNull(templates.getFirst().unavailableReason());
        assertThrows(BusinessException.class, () -> client.sendTemplate(config, "51999888777", templates.getFirst(), List.of()));
        assertTrue(paths.isEmpty());
    }

    @Test void rejectsInvalidParameterSequencesAndDynamicButtons() throws Exception {
        var malformed = WhatsappTemplateParser.approved(mapper.readTree(catalog("APPROVED", "Hola {{2}}"))).getFirst();
        assertFalse(malformed.available());
        var dynamic = WhatsappTemplateParser.approved(mapper.readTree("""
                {"data":[{"name":"url","language":"es_PE","status":"APPROVED","components":[
                {"type":"BODY","text":"Hola"},{"type":"BUTTONS","buttons":[{"type":"URL","url":"https://example.test/{{1}}"}]}]}]}
                """)).getFirst();
        assertFalse(dynamic.available());
    }

    @Test void propagatesMetaRejectionWithoutReturningASuccess() throws Exception {
        var template = WhatsappTemplateParser.approved(mapper.readTree(catalog("APPROVED", "Hola"))).getFirst();
        status = 400;
        response = "{\"error\":{\"message\":\"Unknown rejection\",\"code\":999999}}";
        var error = assertThrows(BusinessException.class, () -> client.sendTemplate(config, "51999888777", template, List.of()));
        assertEquals("CRM_WHATSAPP_META_ERROR", error.getCode());
        assertTrue(error.getMessage().contains("999999"));
        assertTrue(error.getMessage().contains("Unknown rejection"));
        assertFalse(error.getMessage().contains("test-token"));
    }

    @Test void translatesKnownMetaCodesIntoActionableErrorsWithoutLeakingMetaText() throws Exception {
        var template = WhatsappTemplateParser.approved(mapper.readTree(catalog("APPROVED", "Hola"))).getFirst();
        status = 400;
        response = """
                {"error":{"message":"Hello World templates can only be sent from the Public Test Numbers",
                 "code":131058,"error_subcode":2494010}}
                """;
        var error = assertThrows(BusinessException.class, () -> client.sendTemplate(config, "51999888777", template, List.of()));
        assertEquals("CRM_WHATSAPP_PLANTILLA_RECHAZADA", error.getCode());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.isUserActionable());
        assertTrue(error.getMessage().contains("hello_world"));
        assertFalse(error.getMessage().contains("Public Test Numbers"));
        assertFalse(error.getMessage().contains("131058"));
    }

    @Test void mapsMetaThrottlingToTooManyRequests() throws Exception {
        var template = WhatsappTemplateParser.approved(mapper.readTree(catalog("APPROVED", "Hola"))).getFirst();
        status = 400;
        response = "{\"error\":{\"message\":\"Rate limit hit\",\"code\":130429}}";
        var error = assertThrows(BusinessException.class, () -> client.sendTemplate(config, "51999888777", template, List.of()));
        assertEquals("CRM_WHATSAPP_LIMITE_DE_ENVIOS", error.getCode());
        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, error.getStatus());
    }

    @Test void rejectsRepeatedPaginationCursorsAndMalformedResponses() {
        response = "{\"data\":[],\"paging\":{\"next\":\"next\",\"cursors\":{\"after\":\"same\"}}}";
        assertThrows(BusinessException.class, () -> client.listApprovedTemplates(config));
        assertEquals(2, paths.size());
        response = "{}";
        assertThrows(BusinessException.class, () -> client.listApprovedTemplates(config));
    }

    private String catalog(String status, String body) throws Exception {
        var root = mapper.createObjectNode();
        var template = root.putArray("data").addObject();
        template.put("id", "t-1").put("name", "seguimiento_prospecto").put("status", status)
                .put("language", "es_PE").put("category", "MARKETING");
        template.putArray("components").addObject().put("type", "BODY").put("text", body);
        return mapper.writeValueAsString(root);
    }

    private CrmCanalTokenConfig config(String waba, String phone, String token) {
        var config = new CrmCanalTokenConfig();
        config.setWabaId(waba);
        config.setPhoneNumberId(phone);
        config.setAccessToken(token);
        return config;
    }
}
