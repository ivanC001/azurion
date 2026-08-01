package com.azurion.saascore.facturacion.application.services;

import com.azurion.saascore.caja.application.dto.TipoComprobanteVenta;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.shared.exception.BusinessException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FacturadorEmissionCapabilityPolicy {

    private static final Set<String> ELECTRONIC_SUNAT_MODES = Set.of(
            Empresa.FACTURADOR_SUNAT_MODE_BETA,
            Empresa.FACTURADOR_SUNAT_MODE_PRODUCTION
    );

    public void validate(Empresa empresa, TipoComprobanteVenta tipoComprobante) {
        if (!Empresa.FACTURADOR_STATUS_PROVISIONADO.equals(empresa.getFacturadorStatus())) {
            throw new BusinessException(
                    "FACTURADOR_NO_DISPONIBLE",
                    "El facturador de la empresa aun no esta listo. Estado: " + empresa.getFacturadorStatus()
            );
        }

        if (tipoComprobante == TipoComprobanteVenta.TICKET_VENTA) {
            return;
        }

        if (!"PE".equalsIgnoreCase(normalize(empresa.getPaisCodigo()))) {
            throw new BusinessException(
                    "DOCUMENTO_ELECTRONICO_PAIS_NO_PERMITIDO",
                    "Boletas y facturas electronicas solo estan disponibles para empresas de Peru. Emite un ticket de venta."
            );
        }

        boolean electronicActive = Empresa.FACTURADOR_DOCUMENT_MODE_ELECTRONIC.equals(
                empresa.getFacturadorDocumentMode()
        ) && Empresa.FACTURADOR_FISCAL_STATUS_ACTIVE.equals(
                empresa.getFacturadorFiscalStatus()
        ) && ELECTRONIC_SUNAT_MODES.contains(empresa.getFacturadorSunatMode());

        if (!electronicActive) {
            throw new BusinessException(
                    "FACTURACION_ELECTRONICA_NO_CONFIGURADA",
                    "Completa y activa la configuracion fiscal antes de emitir boletas o facturas. Mientras tanto puedes emitir tickets."
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
