package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsappOptOutServiceTest {

    @Mock
    private CrmProspectoRepository prospectoRepository;
    @Mock
    private CrmWhatsappReengagementOutboxRepository outboxRepository;

    @InjectMocks
    private WhatsappOptOutService service;

    @ParameterizedTest
    @ValueSource(strings = {"STOP", "stop", " Stop ", "baja", "Darme de baja", "no molestar",
            "No me escriban", "desuscribirme", "unsubscribe", "Cancelar suscripcion",
            "cancelar suscripción", "¡BAJA!"})
    void reconoceLosPedidosDeBaja(String mensaje) {
        assertTrue(service.esPedidoDeBaja(mensaje));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "no quiero cancelar la compra",
            "me das de baja el precio?",
            "stopper",
            "quiero saber si puedo darme de baja del plan premium mas adelante",
            "hola",
            ""
    })
    void noConfundeMensajesNormalesConUnaBaja(String mensaje) {
        assertFalse(service.esPedidoDeBaja(mensaje));
    }

    @Test
    void marcaLaBajaYCancelaLosReenganchesPendientes() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(7L);
        when(prospectoRepository.findById(7L)).thenReturn(Optional.of(prospecto));

        assertTrue(service.applyIfRequested(prospecto, "STOP"));

        verify(prospectoRepository).save(prospecto);
        assertTrue(prospecto.getWhatsappOptoutEn() != null);
        verify(outboxRepository).cancelPendingForProspecto(
                anyString(), eq(7L), anyString(), any(LocalDateTime.class));
    }

    @Test
    void noTocaNadaCuandoElMensajeNoEsUnaBaja() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(7L);

        assertFalse(service.applyIfRequested(prospecto, "Hola, sigo interesado"));

        verify(prospectoRepository, never()).save(any());
        verify(outboxRepository, never()).cancelPendingForProspecto(
                anyString(), anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void conservaLaFechaDeLaPrimeraBaja() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(7L);
        prospecto.setWhatsappOptoutEn(java.time.OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        when(prospectoRepository.findById(7L)).thenReturn(Optional.of(prospecto));

        service.optOut(7L, "otra vez");

        verify(prospectoRepository, never()).save(any());
        verify(outboxRepository, times(1)).cancelPendingForProspecto(
                anyString(), eq(7L), anyString(), any(LocalDateTime.class));
    }
}
