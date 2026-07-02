package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.LoteResponse;
import com.azurion.saascore.inventory.domain.repositories.LoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProductoLotesUseCase {

    private final LoteRepository loteRepository;

    public List<LoteResponse> execute(Long productoId) {
        return loteRepository.findByProductoIdOrderByFechaVencimientoAscFechaIngresoAsc(productoId).stream()
                .map(lote -> new LoteResponse(
                        lote.getId(),
                        lote.getProducto().getId(),
                        lote.getProducto().getSku(),
                        lote.getProducto().getNombre(),
                        lote.getCodigoLote(),
                        lote.getFechaIngreso(),
                        lote.getFechaVencimiento(),
                        lote.getCantidadInicial(),
                        lote.getCostoUnitario(),
                        lote.getEstado(),
                        lote.getCompraDetalle() == null ? null : lote.getCompraDetalle().getId()
                ))
                .toList();
    }
}
