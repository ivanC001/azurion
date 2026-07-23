package com.azurion.saascore.empresas.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.empresas.application.dto.UpdateCurrentEmpresaProfileRequest;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCurrentEmpresaProfileUseCaseTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private GetCurrentEmpresaUseCase getCurrentEmpresaUseCase;

    @InjectMocks
    private UpdateCurrentEmpresaProfileUseCase useCase;

    @Test
    void shouldNormalizeAndPersistForeignCompanyProfile() {
        Empresa current = company();
        when(getCurrentEmpresaUseCase.resolveCurrentEmpresa()).thenReturn(current);
        when(empresaRepository.findByRucIgnoreCase("US-98/765")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(request(" us-98/765 ", "America/New_York"));

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(captor.capture());
        Empresa saved = captor.getValue();
        assertEquals("US-98/765", saved.getRuc());
        assertEquals("EIN", saved.getTipoDocumentoFiscal());
        assertEquals("US", saved.getPaisCodigo());
        assertEquals("USD", saved.getMonedaCodigo());
        assertEquals("contact@acme.test", saved.getCorreoPrincipal());
        assertEquals("America/New_York", saved.getZonaHoraria());
    }

    @Test
    void shouldRejectInvalidTimezoneBeforeSaving() {
        Empresa current = company();
        when(getCurrentEmpresaUseCase.resolveCurrentEmpresa()).thenReturn(current);
        when(empresaRepository.findByRucIgnoreCase("US-98/765")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> useCase.execute(request("US-98/765", "New_York"))
        );

        assertEquals("EMPRESA_ZONA_HORARIA_INVALIDA", exception.getCode());
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    private Empresa company() {
        Empresa empresa = new Empresa();
        empresa.setId(7L);
        empresa.setRuc("20123456789");
        empresa.setRazonSocial("Empresa actual");
        empresa.setTenantId("empresa_actual");
        empresa.setSchemaName("tenant_empresa_actual");
        return empresa;
    }

    private UpdateCurrentEmpresaProfileRequest request(String fiscalId, String timezone) {
        return new UpdateCurrentEmpresaProfileRequest(
                fiscalId,
                "ACME International LLC",
                "ein",
                "ACME",
                "125 Market Street",
                "New York",
                "New York",
                "New York",
                "us",
                "Estados Unidos",
                " CONTACT@ACME.TEST ",
                "+1 212 555 0100",
                null,
                "https://acme.test",
                null,
                null,
                "Jane Doe",
                "passport",
                "P123456",
                "Manager",
                "legal@acme.test",
                "+1 212 555 0101",
                timezone,
                "en-US",
                "MM/DD/YYYY",
                "12H",
                "usd",
                "$"
        );
    }
}
