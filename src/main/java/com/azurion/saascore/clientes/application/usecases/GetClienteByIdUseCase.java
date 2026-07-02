package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.clientes.application.mappers.ClienteMapper;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetClienteByIdUseCase {

    private final ClienteRepository clienteRepository;

    public ClienteResponse execute(Long id) {
        var cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));

        return ClienteMapper.toResponse(cliente);
    }
}
