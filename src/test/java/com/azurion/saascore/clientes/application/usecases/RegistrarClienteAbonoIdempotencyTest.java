package com.azurion.saascore.clientes.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.clientes.application.dto.ClienteAbonoResponse;
import com.azurion.saascore.clientes.application.dto.RegistrarClienteAbonoRequest;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.entities.ClienteAbono;
import com.azurion.saascore.clientes.domain.repositories.ClienteAbonoRepository;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RegistrarClienteAbonoIdempotencyTest {

    @Test
    void retryReturnsOriginalPaymentWithoutReducingDebtTwice() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClienteAbonoRepository abonoRepository = mock(ClienteAbonoRepository.class);
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setSaldoDeuda(new BigDecimal("100.00"));
        when(clienteRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(cliente));
        when(abonoRepository.findByClientOperationId("payment-123")).thenReturn(Optional.empty());

        AtomicReference<ClienteAbono> saved = new AtomicReference<>();
        when(abonoRepository.saveAndFlush(any(ClienteAbono.class))).thenAnswer(invocation -> {
            ClienteAbono abono = invocation.getArgument(0);
            abono.setId(50L);
            saved.set(abono);
            return abono;
        });
        RegistrarClienteAbonoUseCase useCase = new RegistrarClienteAbonoUseCase(
                clienteRepository,
                abonoRepository
        );
        RegistrarClienteAbonoRequest request = new RegistrarClienteAbonoRequest(
                new BigDecimal("30.00"),
                "Pago parcial",
                "payment-123"
        );

        ClienteAbonoResponse first = useCase.execute(10L, request);
        when(abonoRepository.findByClientOperationId("payment-123"))
                .thenReturn(Optional.of(saved.get()));
        ClienteAbonoResponse repeated = useCase.execute(10L, request);

        assertThat(first.id()).isEqualTo(50L);
        assertThat(repeated.id()).isEqualTo(50L);
        assertThat(cliente.getSaldoDeuda()).isEqualByComparingTo("70.00");
        verify(clienteRepository, times(1)).save(cliente);
        verify(abonoRepository, times(1)).saveAndFlush(any(ClienteAbono.class));
    }
}
