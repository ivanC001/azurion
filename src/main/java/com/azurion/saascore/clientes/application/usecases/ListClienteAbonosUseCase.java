package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteAbonoResponse;
import com.azurion.saascore.clientes.domain.entities.ClienteAbono;
import com.azurion.saascore.clientes.domain.repositories.ClienteAbonoRepository;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListClienteAbonosUseCase {

    private final ClienteRepository clienteRepository;
    private final ClienteAbonoRepository clienteAbonoRepository;

    @Transactional(readOnly = true)
    public List<ClienteAbonoResponse> execute(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado");
        }
        return clienteAbonoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ClienteAbonoResponse toResponse(ClienteAbono abono) {
        return new ClienteAbonoResponse(
                abono.getId(),
                abono.getCliente().getId(),
                abono.getMonto(),
                abono.getSaldoAnterior(),
                abono.getSaldoResultante(),
                abono.getObservacion(),
                abono.getCreatedAt()
        );
    }
}
