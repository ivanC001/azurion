package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.application.dto.UpdateClienteRequest;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.clientes.application.mappers.ClienteMapper;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateClienteUseCase {

    private final ClienteRepository clienteRepository;

    public ClienteResponse execute(Long id, UpdateClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));

        boolean duplicate = clienteRepository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                request.tipoDocumento(),
                request.numeroDocumento(),
                id
        );
        if (duplicate) {
            throw new BusinessException("CLIENTE_DUPLICADO", "Ya existe otro cliente con ese documento");
        }
        if ("6".equals(request.tipoDocumento()) && (request.direccion() == null || request.direccion().isBlank())) {
            throw new BusinessException("DIRECCION_FISCAL_REQUERIDA", "Un cliente con RUC requiere direccion fiscal");
        }

        BigDecimal limiteCredito = request.limiteCredito() == null ? BigDecimal.ZERO : request.limiteCredito();
        BigDecimal saldoDeuda = cliente.getSaldoDeuda() == null ? BigDecimal.ZERO : cliente.getSaldoDeuda();
        if (limiteCredito.compareTo(saldoDeuda) < 0) {
            throw new BusinessException(
                    "LIMITE_MENOR_A_DEUDA",
                    "El limite de credito no puede ser menor a la deuda pendiente"
            );
        }

        cliente.setTipoDocumento(request.tipoDocumento());
        cliente.setNumeroDocumento(request.numeroDocumento());
        cliente.setNombre(request.nombre());
        cliente.setEmail(request.email());
        cliente.setDireccion(request.direccion());
        cliente.setUbigeo(request.ubigeo());
        cliente.setTelefono(request.telefono());
        cliente.setLimiteCredito(limiteCredito);
        cliente.setDiasCredito(request.diasCredito() == null ? 0 : request.diasCredito());
        cliente.setActivo(request.activo() == null || request.activo());

        Cliente saved = clienteRepository.save(cliente);
        return ClienteMapper.toResponse(saved);
    }
}
