package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.clientes.application.mappers.ClienteMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListClientesUseCase {

    private final ClienteRepository clienteRepository;

    public List<ClienteResponse> execute() {
        return clienteRepository.findAll().stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }
}
