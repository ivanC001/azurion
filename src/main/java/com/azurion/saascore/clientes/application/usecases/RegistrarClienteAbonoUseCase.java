package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteAbonoResponse;
import com.azurion.saascore.clientes.application.dto.RegistrarClienteAbonoRequest;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.entities.ClienteAbono;
import com.azurion.saascore.clientes.domain.repositories.ClienteAbonoRepository;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarClienteAbonoUseCase {

    private final ClienteRepository clienteRepository;
    private final ClienteAbonoRepository clienteAbonoRepository;

    @Transactional
    public ClienteAbonoResponse execute(Long clienteId, RegistrarClienteAbonoRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));

        BigDecimal deuda = money(cliente.getSaldoDeuda());
        BigDecimal monto = money(request.monto());
        if (deuda.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("CLIENTE_SIN_DEUDA", "El cliente no tiene deuda pendiente");
        }
        if (monto.compareTo(deuda) > 0) {
            throw new BusinessException("ABONO_EXCEDE_DEUDA", "El abono no puede superar la deuda pendiente");
        }

        BigDecimal saldoResultante = deuda.subtract(monto);
        cliente.setSaldoDeuda(saldoResultante);
        clienteRepository.save(cliente);

        ClienteAbono abono = new ClienteAbono();
        abono.setCliente(cliente);
        abono.setMonto(monto);
        abono.setSaldoAnterior(deuda);
        abono.setSaldoResultante(saldoResultante);
        abono.setObservacion(clean(request.observacion()));

        return toResponse(clienteAbonoRepository.save(abono));
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

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
