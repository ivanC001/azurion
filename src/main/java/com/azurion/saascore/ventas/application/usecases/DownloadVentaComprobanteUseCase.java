package com.azurion.saascore.ventas.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.infrastructure.http.FacturadorClient;
import com.azurion.saascore.ventas.application.dto.FormatoImpresionComprobante;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DownloadVentaComprobanteUseCase {

    private final VentaRepository ventaRepository;
    private final EmpresaRepository empresaRepository;
    private final FacturadorClient facturadorClient;

    public FacturadorClient.FacturadorArtifactDownload execute(
            Long ventaId,
            FormatoImpresionComprobante formato
    ) {
        DownloadContext context = requireDownloadContext(ventaId);
        return facturadorClient.descargarPdfComprobante(
                context.tenantId(),
                context.empresa().getRuc(),
                context.venta().getExternalId(),
                formato
        );
    }

    public FacturadorClient.FacturadorArtifactDownload executeXml(Long ventaId) {
        DownloadContext context = requireDownloadContext(ventaId);
        return facturadorClient.descargarXmlComprobante(
                context.tenantId(),
                context.empresa().getRuc(),
                context.venta().getExternalId()
        );
    }

    public FacturadorClient.FacturadorArtifactDownload executeCdr(Long ventaId) {
        DownloadContext context = requireDownloadContext(ventaId);
        return facturadorClient.descargarCdrComprobante(
                context.tenantId(),
                context.empresa().getRuc(),
                context.venta().getExternalId()
        );
    }

    private DownloadContext requireDownloadContext(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> BusinessException.notFound("VENTA_NOT_FOUND", "La venta no existe"));
        if (Venta.FACTURACION_ESTADO_NO_REQUIERE.equalsIgnoreCase(venta.getFacturacionEstado())) {
            throw BusinessException.conflict(
                    "VENTA_WITHOUT_ELECTRONIC_DOCUMENT",
                    "La venta no tiene un comprobante generado por el facturador"
            );
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || "public".equalsIgnoreCase(tenantId)) {
            throw BusinessException.forbidden("TENANT_REQUIRED", "No existe un tenant activo para descargar el comprobante");
        }
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> BusinessException.notFound("EMPRESA_NOT_FOUND", "No existe la empresa del tenant activo"));
        if (empresa.getRuc() == null || empresa.getRuc().isBlank()) {
            throw new BusinessException("EMPRESA_RUC_REQUIRED", "La empresa debe tener RUC para descargar comprobantes");
        }
        return new DownloadContext(tenantId, empresa, venta);
    }

    private record DownloadContext(String tenantId, Empresa empresa, Venta venta) {
    }
}
