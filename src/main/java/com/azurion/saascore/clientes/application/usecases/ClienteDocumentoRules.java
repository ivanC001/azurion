package com.azurion.saascore.clientes.application.usecases;

import com.azurion.shared.exception.BusinessException;

/**
 * Los codigos SUNAT 1 (DNI) y 6 (RUC) conservan su formato peruano; el resto de
 * documentos (CURP, RFC, CC, NIT, pasaporte, ...) solo pasa la validacion generica del DTO.
 */
final class ClienteDocumentoRules {

    private ClienteDocumentoRules() {
    }

    static void validate(String tipoDocumento, String numeroDocumento) {
        if ("1".equals(tipoDocumento) && !numeroDocumento.matches("\\d{8}")) {
            throw new BusinessException("DOCUMENTO_CLIENTE_INVALIDO", "El DNI debe tener 8 digitos");
        }
        if ("6".equals(tipoDocumento) && !numeroDocumento.matches("\\d{11}")) {
            throw new BusinessException("DOCUMENTO_CLIENTE_INVALIDO", "El RUC debe tener 11 digitos");
        }
    }
}
