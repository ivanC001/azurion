package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.KardexMovimientoResponse;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import java.util.List;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListKardexUseCase {

    private static final int LEGACY_MAX_SIZE = 200;

    private final KardexMovimientoRepository kardexRepository;

    public List<KardexMovimientoResponse> execute(Long productoId, Long almacenId) {
        if (productoId == null && almacenId == null) {
            return page(null, null, 0, LEGACY_MAX_SIZE).content();
        }

        var movimientos = (productoId != null && almacenId != null)
                ? kardexRepository.findByProductoIdAndAlmacenIdOrderByFechaMovimientoDesc(productoId, almacenId)
                : (productoId != null)
                ? kardexRepository.findByProductoIdOrderByFechaMovimientoDesc(productoId)
                : (almacenId != null)
                ? kardexRepository.findByAlmacenIdOrderByFechaMovimientoDesc(almacenId)
                : kardexRepository.findAllByOrderByFechaMovimientoDesc();

        return movimientos.stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<KardexMovimientoResponse> page(
            Long productoId,
            Long almacenId,
            int page,
            int size
    ) {
        var result = kardexRepository.search(
                productoId,
                almacenId,
                PageRequestSupport.of(page, size, Sort.by("fechaMovimiento").descending())
        );
        return PageResponse.from(result, result.getContent().stream().map(this::toResponse).toList());
    }

    private KardexMovimientoResponse toResponse(
            com.azurion.saascore.inventory.domain.entities.KardexMovimiento movimiento
    ) {
        return new KardexMovimientoResponse(
                movimiento.getId(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getSku(),
                movimiento.getProducto().getNombre(),
                movimiento.getAlmacen().getId(),
                movimiento.getAlmacen().getCodigo(),
                movimiento.getTipoMovimiento(),
                movimiento.getMotivo(),
                movimiento.getCantidad(),
                movimiento.getSaldoResultante(),
                movimiento.getReferencia(),
                movimiento.getFechaMovimiento()
        );
    }
}
