package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.application.mappers.CompraInventoryMapper;
import com.azurion.saascore.inventory.domain.repositories.CompraDetalleRepository;
import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListComprasUseCase {

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;

    @Transactional(readOnly = true)
    public List<CompraResponse> execute() {
        return page("", null, 0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<CompraResponse> page(
            String query,
            Long almacenId,
            int page,
            int size
    ) {
        var result = compraRepository.search(
                query == null ? "" : query.trim(),
                almacenId,
                PageRequestSupport.of(page, size, Sort.by("fechaIngreso").descending())
        );
        List<Long> compraIds = result.getContent().stream().map(compra -> compra.getId()).toList();
        Map<Long, List<com.azurion.saascore.inventory.domain.entities.CompraDetalle>> detalles =
                compraIds.isEmpty()
                        ? Map.of()
                        : compraDetalleRepository.findByCompraIdIn(compraIds).stream()
                                .collect(Collectors.groupingBy(detalle -> detalle.getCompra().getId()));
        List<CompraResponse> content = result.getContent().stream()
                .map(compra -> CompraInventoryMapper.toResponse(
                        compra,
                        detalles.getOrDefault(compra.getId(), List.of())
                ))
                .toList();
        return PageResponse.from(result, content);
    }
}
