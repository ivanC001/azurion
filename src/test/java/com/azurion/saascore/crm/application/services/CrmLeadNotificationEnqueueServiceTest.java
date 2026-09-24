package com.azurion.saascore.crm.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.events.CrmLeadNotificationQueuedEvent;
import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationConfig;
import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationDispatch;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationDispatchRepository;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CrmLeadNotificationEnqueueServiceTest {

    @Mock
    private CrmLeadNotificationConfigRepository configRepository;
    @Mock
    private CrmLeadNotificationDispatchRepository dispatchRepository;
    @Mock
    private UsuarioTenantRepository usuarioTenantRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CrmLeadNotificationEnqueueService service;

    @BeforeEach
    void setUp() {
        service = new CrmLeadNotificationEnqueueService(
                configRepository,
                dispatchRepository,
                usuarioTenantRepository,
                eventPublisher
        );
    }

    @Test
    void avisaAlAsesorAsignadoYAlosCorreosDeCopia() {
        CrmLeadNotificationConfig config = activeConfig();
        config.setCorreosCopia("supervision@empresa.test");
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(dispatchRepository.existsRecent(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        when(usuarioTenantRepository.findById(7L)).thenReturn(Optional.of(advisor("Vendedor@Empresa.test")));
        when(dispatchRepository.save(any(CrmLeadNotificationDispatch.class)))
                .thenAnswer(invocation -> {
                    CrmLeadNotificationDispatch dispatch = invocation.getArgument(0);
                    dispatch.setId(15L);
                    return dispatch;
                });

        service.enqueue(prospecto("7"), CrmLeadNotificationEnqueueService.TIPO_LEAD_NUEVO, "Nuevo lead CRM: Jose");

        ArgumentCaptor<CrmLeadNotificationDispatch> captor = ArgumentCaptor.forClass(CrmLeadNotificationDispatch.class);
        verify(dispatchRepository).save(captor.capture());
        assertThat(captor.getValue().getDestinatarios()).isEqualTo("vendedor@empresa.test,supervision@empresa.test");
        assertThat(captor.getValue().getEstado()).isEqualTo("PENDIENTE");
        verify(eventPublisher).publishEvent(any(CrmLeadNotificationQueuedEvent.class));
    }

    @Test
    void noEncolaCuandoElAvisoEstaApagado() {
        CrmLeadNotificationConfig config = activeConfig();
        config.setActivo(false);
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));

        service.enqueue(prospecto("7"), CrmLeadNotificationEnqueueService.TIPO_LEAD_NUEVO, "Nuevo lead");

        verify(dispatchRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(CrmLeadNotificationQueuedEvent.class));
    }

    @Test
    void elCooldownEvitaAvisarDosVecesPorElMismoProspecto() {
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(activeConfig()));
        when(dispatchRepository.existsRecent(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(true);

        service.enqueue(prospecto("7"), CrmLeadNotificationEnqueueService.TIPO_MENSAJE_NUEVO, "Mensaje nuevo");

        verify(dispatchRepository, never()).save(any());
    }

    @Test
    void sinDestinatariosNoSeEncolaNada() {
        CrmLeadNotificationConfig config = activeConfig();
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(dispatchRepository.existsRecent(anyLong(), anyString(), any(LocalDateTime.class))).thenReturn(false);
        // El duenio ficticio de los leads publicos no es un usuario con correo.
        when(usuarioTenantRepository.findByUsernameAndActivoTrue("crm-public")).thenReturn(Optional.empty());

        service.enqueue(prospecto("crm-public"), CrmLeadNotificationEnqueueService.TIPO_LEAD_NUEVO, "Nuevo lead");

        verify(dispatchRepository, never()).save(any());
    }

    @Test
    void unFalloAvisandoNoRompeElRegistroDelLead() {
        when(configRepository.findFirstByOrderByIdAsc()).thenThrow(new IllegalStateException("base caida"));

        assertThatCode(() -> service.enqueue(
                prospecto("7"),
                CrmLeadNotificationEnqueueService.TIPO_LEAD_NUEVO,
                "Nuevo lead"))
                .doesNotThrowAnyException();
    }

    private CrmLeadNotificationConfig activeConfig() {
        CrmLeadNotificationConfig config = new CrmLeadNotificationConfig();
        config.setActivo(true);
        config.setNotificarLeadNuevo(true);
        config.setNotificarMensajeNuevo(true);
        config.setNotificarResponsable(true);
        config.setCooldownMinutos(30);
        return config;
    }

    private UsuarioTenant advisor(String email) {
        UsuarioTenant usuario = new UsuarioTenant();
        usuario.setId(7L);
        usuario.setUsername("vendedor1");
        usuario.setEmail(email);
        usuario.setActivo(true);
        return usuario;
    }

    private CrmProspecto prospecto(String responsableId) {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(31L);
        prospecto.setNombre("Jose Ramirez");
        prospecto.setResponsableId(responsableId);
        return prospecto;
    }
}
