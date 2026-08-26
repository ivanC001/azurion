package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.SaveWhatsappQuickReplyRequest;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappQuickReply;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappQuickReplyRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class WhatsappQuickReplyServiceTest {

    @Mock
    private CrmWhatsappQuickReplyRepository repository;

    private WhatsappQuickReplyService service;

    @BeforeEach
    void setUp() {
        service = new WhatsappQuickReplyService(repository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("asesor.demo", "n/a", List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsReplyInFirstAvailableSlotForCurrentAdvisor() {
        when(repository.findAllByUsuarioIdOrderBySlotAsc("asesor.demo"))
                .thenReturn(List.of(reply(10L, 1), reply(12L, 3)));
        when(repository.save(any(CrmWhatsappQuickReply.class))).thenAnswer(invocation -> {
            CrmWhatsappQuickReply saved = invocation.getArgument(0);
            saved.setId(13L);
            return saved;
        });

        var response = service.create(new SaveWhatsappQuickReplyRequest("Saludo", "Hola, te ayudamos."));

        assertEquals(2, response.slot());
        ArgumentCaptor<CrmWhatsappQuickReply> captor = ArgumentCaptor.forClass(CrmWhatsappQuickReply.class);
        verify(repository).save(captor.capture());
        assertEquals("asesor.demo", captor.getValue().getUsuarioId());
    }

    @Test
    void rejectsFourthReply() {
        when(repository.findAllByUsuarioIdOrderBySlotAsc("asesor.demo"))
                .thenReturn(List.of(reply(1L, 1), reply(2L, 2), reply(3L, 3)));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.create(new SaveWhatsappQuickReplyRequest("Extra", "No debe guardarse"))
        );

        assertEquals("CRM_WHATSAPP_RESPUESTAS_RAPIDAS_LIMITE", error.getCode());
    }

    @Test
    void cannotEditReplyOwnedByAnotherAdvisor() {
        when(repository.findByIdAndUsuarioId(99L, "asesor.demo")).thenReturn(Optional.empty());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.update(99L, new SaveWhatsappQuickReplyRequest("Titulo", "Mensaje"))
        );

        assertEquals("CRM_WHATSAPP_RESPUESTA_RAPIDA_NO_ENCONTRADA", error.getCode());
        verify(repository).findByIdAndUsuarioId(99L, "asesor.demo");
    }

    private CrmWhatsappQuickReply reply(Long id, int slot) {
        CrmWhatsappQuickReply reply = new CrmWhatsappQuickReply();
        reply.setId(id);
        reply.setUsuarioId("asesor.demo");
        reply.setSlot(slot);
        reply.setTitulo("Respuesta " + slot);
        reply.setMensaje("Mensaje " + slot);
        return reply;
    }
}
