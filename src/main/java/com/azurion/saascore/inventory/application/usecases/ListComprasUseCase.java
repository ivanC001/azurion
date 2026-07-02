package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.application.mappers.CompraInventoryMapper;
import com.azurion.saascore.inventory.domain.repositories.CompraDetalleRepository;
import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import java.util.List;
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
        return compraRepository.findAllByOrderByFechaIngresoDesc().stream()
                .map(compra -> CompraInventoryMapper.toResponse(compra, compraDetalleRepository.findByCompraId(compra.getId())))
                .toList();
    }
}
