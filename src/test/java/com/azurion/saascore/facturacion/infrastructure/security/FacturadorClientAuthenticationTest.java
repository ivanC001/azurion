package com.azurion.saascore.facturacion.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azurion.saascore.facturacion.infrastructure.config.FacturadorProperties;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FacturadorClientAuthenticationTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsClientIdAndV2SignatureWithoutExposingSecret() throws Exception {
        AtomicReference<HttpExchange> captured = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/api/integrations/azurion/tenants/tenant-a",
                exchange -> respond(exchange, captured, body)
        );
        server.start();

        FacturadorProperties properties = new FacturadorProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setClientId("azurion-core");
        properties.setClientSecret("server-only-secret");
        properties.setSignatureVersion("v2");
        FacturadorHmacSigner signer = new FacturadorHmacSigner();
        FacturadorClient client = new FacturadorClient(properties, new ObjectMapper(), signer);

        client.provisionarTenant("tenant-a", "Tenant A", "PE", "20111111111", true);

        HttpExchange request = captured.get();
        assertEquals("azurion-core", request.getRequestHeaders().getFirst("X-Client-Id"));
        assertEquals("v2", request.getRequestHeaders().getFirst("X-Signature-Version"));
        assertEquals("tenant-a", request.getRequestHeaders().getFirst("X-Azurion-Tenant-ID"));
        assertNull(request.getRequestHeaders().getFirst("X-API-Key"));

        String expected = signer.sign(
                "v2",
                "PUT",
                request.getRequestURI().toString(),
                request.getRequestHeaders().getFirst("X-Timestamp"),
                request.getRequestHeaders().getFirst("X-Nonce"),
                "tenant-a",
                "",
                body.get(),
                "server-only-secret"
        );
        assertEquals(expected, request.getRequestHeaders().getFirst("X-Signature"));
    }

    @Test
    void extractsIdempotencyKeyFromNestedFiscalDocument() throws Exception {
        FacturadorClient client = new FacturadorClient(
                new FacturadorProperties(),
                new ObjectMapper(),
                new FacturadorHmacSigner()
        );
        Method extractor = FacturadorClient.class
                .getDeclaredMethod("extractIdempotencyKey", String.class);
        extractor.setAccessible(true);

        String key = (String) extractor.invoke(
                client,
                "{\"documento\":{\"external_id\":\"GUIA-ABC123\"}}"
        );

        assertEquals("GUIA-ABC123", key);
    }

    @Test
    void downloadsPdfUsingFreshArtifactUrlReturnedByFacturador() throws Exception {
        byte[] pdf = "%PDF-1.4\ntest-pdf".getBytes(StandardCharsets.US_ASCII);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/documentos", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/api/documentos/3/pdf".equals(path)) {
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.sendResponseHeaders(200, pdf.length);
                exchange.getResponseBody().write(pdf);
                exchange.close();
                return;
            }

            int port = server.getAddress().getPort();
            byte[] response = ("""
                    {"success":true,"data":{"items":[{
                      "id":3,
                      "external_id":"VENTA-3",
                      "tipo_documento":"03",
                      "serie":"B001",
                      "correlativo":"1",
                      "estado":"ACEPTADO",
                      "pdf_url":"http://127.0.0.1:%d/api/documentos/3/pdf?expires=9999999999&tenant_ruc=20000000001&signature=%s"
                    }]}}
                    """).formatted(port, "a".repeat(64)).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        FacturadorProperties properties = new FacturadorProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setClientId("azurion-core");
        properties.setClientSecret("server-only-secret");
        properties.setSignatureVersion("v2");
        FacturadorClient client = new FacturadorClient(
                properties,
                new ObjectMapper(),
                new FacturadorHmacSigner()
        );

        FacturadorClient.FacturadorArtifactDownload result = client.descargarPdfComprobante(
                "tenant-a",
                "20000000001",
                "VENTA-3"
        );

        assertEquals("20000000001-03-B001-1.pdf", result.filename());
        assertEquals("application/pdf", result.contentType());
        assertTrue(java.util.Arrays.equals(pdf, result.content()));
    }

    @Test
    void acceptsConfiguredPublicArtifactOriginDifferentFromInternalApiOrigin() throws Exception {
        byte[] pdf = "%PDF-1.4\npublic-origin".getBytes(StandardCharsets.US_ASCII);
        HttpServer artifactServer = HttpServer.create(new InetSocketAddress(0), 0);
        artifactServer.createContext("/api/documentos/9/pdf", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, pdf.length);
            exchange.getResponseBody().write(pdf);
            exchange.close();
        });
        artifactServer.start();

        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/api/documentos", exchange -> {
                int artifactPort = artifactServer.getAddress().getPort();
                byte[] response = ("""
                        {"success":true,"data":{"items":[{
                          "id":9,
                          "external_id":"VENTA-9",
                          "tipo_documento":"01",
                          "serie":"F001",
                          "correlativo":"4",
                          "estado":"ACEPTADO",
                          "pdf_url":"http://127.0.0.1:%d/api/documentos/9/pdf?expires=9999999999&tenant_ruc=20111111111&signature=%s"
                        }]}}
                        """).formatted(artifactPort, "b".repeat(64)).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            FacturadorProperties properties = new FacturadorProperties();
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setArtifactBaseUrl("http://127.0.0.1:" + artifactServer.getAddress().getPort());
            properties.setClientSecret("server-only-secret");
            FacturadorClient client = new FacturadorClient(
                    properties,
                    new ObjectMapper(),
                    new FacturadorHmacSigner()
            );

            FacturadorClient.FacturadorArtifactDownload result = client.descargarPdfComprobante(
                    "tenant-a",
                    "20111111111",
                    "VENTA-9"
            );

            assertEquals("20111111111-01-F001-4.pdf", result.filename());
            assertTrue(java.util.Arrays.equals(pdf, result.content()));
        } finally {
            artifactServer.stop(0);
        }
    }

    private void respond(
            HttpExchange exchange,
            AtomicReference<HttpExchange> captured,
            AtomicReference<String> requestBody
    ) throws IOException {
        captured.set(exchange);
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = """
                {"success":true,"data":{
                  "document_mode":"TICKET_ONLY",
                  "fiscal_status":"NOT_CONFIGURED",
                  "sunat_mode":"disabled"
                }}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
