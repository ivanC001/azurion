package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.KardexMovimientoResponse;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListLoteMovimientosUseCase {

    private final KardexMovimientoRepository kardexRepository;

    public List<KardexMovimientoResponse> execute(Long loteId) {
        return kardexRepository.findByLoteIdOrderByFechaMovimientoDesc(loteId).stream()
                .map(k -> new KardexMovimientoResponse(
                        k.getId(),
                        k.getProducto().getId(),
                        k.getProducto().getSku(),
                        k.getProducto().getNombre(),
                        k.getAlmacen().getId(),
                        k.getAlmacen().getCodigo(),
                        k.getTipoMovimiento(),
                        k.getMotivo(),
                        k.getCantidad(),
                        k.getSaldoResultante(),
                        k.getReferencia(),
                        k.getFechaMovimiento()
                ))
                .toList();
    }
}
