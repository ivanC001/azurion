package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteAbonoRepository;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final ClienteAbonoRepository clienteAbonoRepository;

    public void execute(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));
        BigDecimal deuda = cliente.getSaldoDeuda() == null ? BigDecimal.ZERO : cliente.getSaldoDeuda();
        if (deuda.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("CLIENTE_CON_DEUDA", "No se puede eliminar un cliente con deuda pendiente");
        }
        if (clienteAbonoRepository.existsByClienteId(id)) {
            throw new BusinessException("CLIENTE_CON_HISTORIAL", "No se puede eliminar un cliente con historial de abonos; desactivalo");
        }
        clienteRepository.deleteById(id);
    }
}
