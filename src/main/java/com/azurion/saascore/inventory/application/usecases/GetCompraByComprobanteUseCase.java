package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.application.mappers.CompraInventoryMapper;
import com.azurion.saascore.inventory.domain.entities.Compra;
import com.azurion.saascore.inventory.domain.repositories.CompraDetalleRepository;
import com.azurion.saascore.inventory.domain.repositories.CompraRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCompraByComprobanteUseCase {

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;

    public CompraResponse execute(String numeroComprobante) {
        Compra compra = compraRepository.findFirstByNumeroComprobanteIgnoreCase(numeroComprobante)
                .orElseThrow(() -> new BusinessException("COMPRA_NO_ENCONTRADA", "Compra no encontrada"));
        return CompraInventoryMapper.toResponse(compra, compraDetalleRepository.findByCompraId(compra.getId()));
    }
}
