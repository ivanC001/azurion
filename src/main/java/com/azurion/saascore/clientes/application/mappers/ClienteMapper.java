package com.azurion.saascore.clientes.application.mappers;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import java.math.BigDecimal;

public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static ClienteResponse toResponse(Cliente cliente) {
        BigDecimal limite = value(cliente.getLimiteCredito());
        BigDecimal deuda = value(cliente.getSaldoDeuda());

        return new ClienteResponse(
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getUbigeo(),
                cliente.getTelefono(),
                limite,
                deuda,
                limite.subtract(deuda).max(BigDecimal.ZERO),
                cliente.getDiasCredito() == null ? 0 : cliente.getDiasCredito(),
                deuda.compareTo(BigDecimal.ZERO) > 0,
                cliente.getActivo() == null || cliente.getActivo()
        );
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
