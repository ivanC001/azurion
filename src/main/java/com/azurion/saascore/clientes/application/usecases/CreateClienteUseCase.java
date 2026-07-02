package com.azurion.saascore.clientes.application.usecases;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.application.dto.CreateClienteRequest;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.clientes.application.mappers.ClienteMapper;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateClienteUseCase {

    private final ClienteRepository clienteRepository;

    public ClienteResponse execute(CreateClienteRequest request) {
        validateFiscalData(request.tipoDocumento(), request.direccion());
        clienteRepository.findByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), request.numeroDocumento())
                .ifPresent(existing -> {
                    throw new BusinessException("CLIENTE_DUPLICADO", "Ya existe un cliente con ese documento");
                });

        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(request.tipoDocumento());
        cliente.setNumeroDocumento(request.numeroDocumento());
        cliente.setNombre(request.nombre());
        cliente.setEmail(request.email());
        cliente.setDireccion(request.direccion());
        cliente.setUbigeo(request.ubigeo());
        cliente.setTelefono(request.telefono());
        cliente.setLimiteCredito(request.limiteCredito() == null ? BigDecimal.ZERO : request.limiteCredito());
        cliente.setSaldoDeuda(BigDecimal.ZERO);
        cliente.setDiasCredito(request.diasCredito() == null ? 0 : request.diasCredito());
        cliente.setActivo(request.activo() == null || request.activo());

        Cliente saved = clienteRepository.save(cliente);
        return ClienteMapper.toResponse(saved);
    }

    private void validateFiscalData(String tipoDocumento, String direccion) {
        if ("6".equals(tipoDocumento) && (direccion == null || direccion.isBlank())) {
            throw new BusinessException("DIRECCION_FISCAL_REQUERIDA", "Un cliente con RUC requiere direccion fiscal");
        }
    }
}
