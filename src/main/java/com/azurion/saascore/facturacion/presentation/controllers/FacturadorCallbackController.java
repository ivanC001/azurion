package com.azurion.saascore.facturacion.presentation.controllers;

import com.azurion.saascore.facturacion.application.usecases.ProcessFacturadorVentaCallbackUseCase;
import com.azurion.saascore.facturacion.infrastructure.security.FacturadorCallbackVerifier;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/facturador/callback")
@RequiredArgsConstructor
public class FacturadorCallbackController {

    private static final String CALLBACK_VENTAS = "VENTAS";
    private static final String CALLBACK_DOCUMENTOS = "DOCUMENTOS";
    private static final String CALLBACK_GUIAS = "GUIAS";
    private static final String CALLBACK_NOTAS_CREDITO = "NOTAS_CREDITO";
    private static final String CALLBACK_NOTAS_DEBITO = "NOTAS_DEBITO";

    private final FacturadorCallbackVerifier callbackVerifier;
    private final ProcessFacturadorVentaCallbackUseCase processFacturadorVentaCallbackUseCase;
    private final ObjectMapper objectMapper;

    @PostMapping("/ventas")
    public ApiResponse<Map<String, Object>> callbackVenta(HttpServletRequest request,
                                                          @RequestBody(required = false) String rawBody) {
        return processCallback(request, rawBody, CALLBACK_VENTAS);
    }

    @PostMapping("/documentos")
    public ApiResponse<Map<String, Object>> callbackDocumentos(HttpServletRequest request,
                                                               @RequestBody(required = false) String rawBody) {
        return processCallback(request, rawBody, CALLBACK_DOCUMENTOS);
    }

    @PostMapping("/guias")
    public ApiResponse<Map<String, Object>> callbackGuias(HttpServletRequest request,
                                                          @RequestBody(required = false) String rawBody) {
        return processCallback(request, rawBody, CALLBACK_GUIAS);
    }

    @PostMapping("/notas-credito")
    public ApiResponse<Map<String, Object>> callbackNotasCredito(HttpServletRequest request,
                                                                 @RequestBody(required = false) String rawBody) {
        return processCallback(request, rawBody, CALLBACK_NOTAS_CREDITO);
    }

    @PostMapping("/notas-debito")
    public ApiResponse<Map<String, Object>> callbackNotasDebito(HttpServletRequest request,
                                                                @RequestBody(required = false) String rawBody) {
        return processCallback(request, rawBody, CALLBACK_NOTAS_DEBITO);
    }

    private ApiResponse<Map<String, Object>> processCallback(HttpServletRequest request,
                                                             String rawBody,
                                                             String callbackChannel) {
        String body = rawBody == null ? "" : rawBody;
        callbackVerifier.verify(request, body);

        JsonNode payload = parsePayload(body);
        ProcessFacturadorVentaCallbackUseCase.CallbackProcessResult result =
                processFacturadorVentaCallbackUseCase.execute(payload, callbackChannel);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("processed", true);
        data.put("callbackChannel", result.callbackChannel());
        data.put("tipoDocumento", result.tipoDocumento());
        data.put("tenantId", result.tenantId());
        data.put("ventaId", result.ventaId());
        data.put("externalId", result.externalId());
        data.put("estado", result.estado());

        return ApiResponse.ok(data, "Callback de facturador procesado");
    }

    private JsonNode parsePayload(String rawBody) {
        if (rawBody == null || rawBody.trim().isBlank()) {
            throw new BusinessException("FACTURADOR_CALLBACK_BODY_EMPTY", "El callback no contiene body JSON.");
        }
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            if (node == null || !node.isObject()) {
                throw new BusinessException("FACTURADOR_CALLBACK_BODY_INVALID", "El body del callback debe ser un JSON object.");
            }
            return node;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception exception) {
            throw new BusinessException("FACTURADOR_CALLBACK_JSON_INVALID", "No se pudo parsear el JSON del callback.");
        }
    }
}
