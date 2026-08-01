package com.azurion.saascore.facturacion.application.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azurion.saascore.caja.application.dto.TipoComprobanteVenta;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacturadorEmissionCapabilityPolicyTest {

    private FacturadorEmissionCapabilityPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new FacturadorEmissionCapabilityPolicy();
    }

    @Test
    void foreignTenantCanIssueInternalTicket() {
        Empresa empresa = baseEmpresa("US");

        assertDoesNotThrow(() -> policy.validate(empresa, TipoComprobanteVenta.TICKET_VENTA));
    }

    @Test
    void foreignTenantCannotIssueElectronicReceipt() {
        Empresa empresa = electronicEmpresa("US");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validate(empresa, TipoComprobanteVenta.BOLETA)
        );

        assertEquals("DOCUMENTO_ELECTRONICO_PAIS_NO_PERMITIDO", exception.getCode());
    }

    @Test
    void peruTenantMustCompleteFiscalConfigurationBeforeElectronicEmission() {
        Empresa empresa = baseEmpresa("PE");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validate(empresa, TipoComprobanteVenta.FACTURA)
        );

        assertEquals("FACTURACION_ELECTRONICA_NO_CONFIGURADA", exception.getCode());
    }

    @Test
    void peruTenantWithActiveProductionConfigurationCanIssueElectronicDocuments() {
        Empresa empresa = electronicEmpresa("PE");

        assertDoesNotThrow(() -> policy.validate(empresa, TipoComprobanteVenta.FACTURA));
        assertDoesNotThrow(() -> policy.validate(empresa, TipoComprobanteVenta.BOLETA));
    }

    @Test
    void noDocumentCanBeIssuedBeforeProvisioningCompletes() {
        Empresa empresa = baseEmpresa("PE");
        empresa.setFacturadorStatus(Empresa.FACTURADOR_STATUS_PENDIENTE);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validate(empresa, TipoComprobanteVenta.TICKET_VENTA)
        );

        assertEquals("FACTURADOR_NO_DISPONIBLE", exception.getCode());
    }

    private Empresa baseEmpresa(String countryCode) {
        Empresa empresa = new Empresa();
        empresa.setPaisCodigo(countryCode);
        empresa.setFacturadorStatus(Empresa.FACTURADOR_STATUS_PROVISIONADO);
        empresa.setFacturadorDocumentMode(Empresa.FACTURADOR_DOCUMENT_MODE_TICKET_ONLY);
        empresa.setFacturadorFiscalStatus(Empresa.FACTURADOR_FISCAL_STATUS_NOT_CONFIGURED);
        empresa.setFacturadorSunatMode(Empresa.FACTURADOR_SUNAT_MODE_DISABLED);
        return empresa;
    }

    private Empresa electronicEmpresa(String countryCode) {
        Empresa empresa = baseEmpresa(countryCode);
        empresa.setFacturadorDocumentMode(Empresa.FACTURADOR_DOCUMENT_MODE_ELECTRONIC);
        empresa.setFacturadorFiscalStatus(Empresa.FACTURADOR_FISCAL_STATUS_ACTIVE);
        empresa.setFacturadorSunatMode(Empresa.FACTURADOR_SUNAT_MODE_PRODUCTION);
        return empresa;
    }
}
