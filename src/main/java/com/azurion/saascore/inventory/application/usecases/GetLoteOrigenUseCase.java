package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.LoteOrigenResponse;
import com.azurion.saascore.inventory.domain.entities.Compra;
import com.azurion.saascore.inventory.domain.entities.CompraDetalle;
import com.azurion.saascore.inventory.domain.entities.Lote;
import com.azurion.saascore.inventory.domain.repositories.LoteRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLoteOrigenUseCase {

    private final LoteRepository loteRepository;

    public LoteOrigenResponse execute(Long loteId) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new BusinessException("LOTE_NO_ENCONTRADO", "Lote no encontrado"));
        CompraDetalle detalle = lote.getCompraDetalle();
        Compra compra = detalle == null ? null : detalle.getCompra();
        return new LoteOrigenResponse(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getProducto().getId(),
                lote.getProducto().getSku(),
                lote.getProducto().getNombre(),
                lote.getFechaIngreso(),
                lote.getFechaVencimiento(),
                lote.getCantidadInicial(),
                lote.getCostoUnitario(),
                compra == null ? null : compra.getId(),
                compra == null ? null : compra.getTipoComprobante(),
                compra == null ? null : compra.getNumeroComprobante(),
                compra == null ? null : compra.getFechaEmision(),
                compra == null ? lote.getProveedorId() : compra.getProveedorId(),
                compra == null ? null : compra.getProveedorDocumento(),
                compra == null ? null : compra.getProveedorNombre()
        );
    }
}
