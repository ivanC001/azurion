package com.azurion.saascore.facturacion.application.services;

import com.azurion.saascore.facturacion.application.dto.FacturadorTenantConfigurationRequest;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FacturadorTenantPayloadFactory {

    private static final long MAX_LOGO_BYTES = 2L * 1024L * 1024L;
    private static final long MAX_CERTIFICATE_BYTES = 5L * 1024L * 1024L;
    private final ObjectMapper objectMapper;

    public Map<String, Object> create(FacturadorTenantConfigurationRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putText(payload, "ruc", request.getRuc());
        putText(payload, "business_name", request.getBusiness_name());
        putText(payload, "external_tenant_id", request.getExternal_tenant_id());
        putText(payload, "country_code", request.getCountry_code());
        putText(payload, "tax_id", request.getTax_id());
        putText(payload, "sunat_mode", request.getSunat_mode());
        putText(payload, "api_client_name", request.getApi_client_name());
        putText(payload, "ruc_sol", request.getRuc_sol());
        putText(payload, "usuario_sol", request.getUsuario_sol());
        putText(payload, "clave_sol", request.getClave_sol());
        putText(payload, "certificado_password", request.getCertificado_password());
        putText(payload, "serie_factura", request.getSerie_factura());
        putText(payload, "serie_boleta", request.getSerie_boleta());
        putText(payload, "serie_nc", request.getSerie_nc());
        putText(payload, "serie_nd", request.getSerie_nd());
        putText(payload, "serie_guia", request.getSerie_guia());
        putText(payload, "moneda", request.getMoneda());
        if (request.getIgv() != null) {
            payload.put("igv", request.getIgv());
        }
        addBankAccounts(payload, request.getCuentas_bancarias_json());
        addFile(payload, request.getLogo_file(), "logo_file", MAX_LOGO_BYTES, "png", "jpg", "jpeg", "webp");
        addFile(
                payload,
                request.getCertificado_file(),
                "certificado_file",
                MAX_CERTIFICATE_BYTES,
                "pem",
                "pfx",
                "p12"
        );
        return payload;
    }

    private void addBankAccounts(Map<String, Object> payload, String rawJson) {
        if (rawJson == null) {
            return;
        }
        if (rawJson.isBlank()) {
            payload.put("cuentas_bancarias", List.of());
            return;
        }

        try {
            JsonNode source = objectMapper.readTree(rawJson);
            if (!source.isArray() || source.size() > 3) {
                throw invalidBankAccounts();
            }

            List<Map<String, String>> accounts = new ArrayList<>();
            for (JsonNode item : source) {
                String bank = requiredText(item, "banco", 120);
                String currency = requiredText(item, "moneda", 3).toUpperCase();
                String account = requiredText(item, "cuenta", 50);
                String cci = requiredText(item, "cci", 34);
                if (!currency.matches("^[A-Z]{3}$")
                        || !account.matches("^[A-Za-z0-9 .-]+$")
                        || !cci.matches("^[A-Za-z0-9 .-]+$")) {
                    throw invalidBankAccounts();
                }

                Map<String, String> normalized = new LinkedHashMap<>();
                normalized.put("banco", bank);
                normalized.put("moneda", currency);
                normalized.put("cuenta", account);
                normalized.put("cci", cci);
                accounts.add(normalized);
            }
            payload.put("cuentas_bancarias", accounts);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidBankAccounts();
        }
    }

    private String requiredText(JsonNode source, String field, int maxLength) {
        String value = source.path(field).asText("").trim();
        if (value.isBlank() || value.length() > maxLength) {
            throw invalidBankAccounts();
        }
        return value;
    }

    private BusinessException invalidBankAccounts() {
        return new BusinessException(
                "FACTURADOR_BANK_ACCOUNTS_INVALID",
                "Las cuentas bancarias deben incluir banco, moneda, numero de cuenta y codigo CCI"
        );
    }

    private void putText(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value.trim());
        }
    }

    private void addFile(
            Map<String, Object> payload,
            MultipartFile file,
            String field,
            long maxBytes,
            String... allowedExtensions
    ) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        boolean allowed = java.util.Arrays.stream(allowedExtensions).anyMatch(extension::equals);
        if (!allowed || file.getSize() > maxBytes) {
            throw new BusinessException("FACTURADOR_FILE_INVALID", "El archivo " + field + " no es valido");
        }
        try {
            payload.put(field + "_base64", Base64.getEncoder().encodeToString(file.getBytes()));
            payload.put(field + "_name", name);
        } catch (IOException exception) {
            throw new BusinessException("FACTURADOR_FILE_ERROR", "No se pudo leer el archivo " + field);
        }
    }
}
