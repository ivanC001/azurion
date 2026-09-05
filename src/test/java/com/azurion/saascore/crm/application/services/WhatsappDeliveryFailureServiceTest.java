package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappMessage;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappMessageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WhatsappDeliveryFailureServiceTest {

    @Mock
    private CrmWhatsappMessageRepository messageRepository;

    @InjectMocks
    private WhatsappDeliveryFailureService service;

    private void devuelve(CrmWhatsappMessage... mensajes) {
        when(messageRepository.findAllByDireccionAndEstadoOrderByMensajeEnDescIdDesc(
                eq("SALIENTE"), eq("FALLIDO"), any(Pageable.class)))
                .thenReturn(List.of(mensajes));
    }

    @Test
    void traduceElCodigoDeMetaAAlgoAccionable() {
        devuelve(mensaje("131042",
                "Message failed to send because no payment method is set up for your account."));

        var fallo = service.recentFailures().getFirst();

        assertEquals("131042", fallo.codigo());
        assertTrue(fallo.causa().contains("metodo de pago"));
        assertTrue(fallo.solucion().contains("Facturacion"));
        // El texto original de Meta se conserva por si hace falta el detalle exacto.
        assertTrue(fallo.detalle().contains("no payment method"));
    }

    @Test
    void noInventaUnaCausaCuandoElCodigoNoEstaEnElCatalogo() {
        devuelve(mensaje("999999", "Algo raro paso"));

        var fallo = service.recentFailures().getFirst();

        assertEquals("Meta no pudo entregar el mensaje.", fallo.causa());
        assertNull(fallo.solucion());
        assertEquals("Algo raro paso", fallo.detalle());
    }

    @Test
    void toleraUnMensajeSinCodigoNiProspecto() {
        CrmWhatsappMessage sinDatos = mensaje(null, null);
        sinDatos.setProspecto(null);
        devuelve(sinDatos);

        var fallo = service.recentFailures().getFirst();

        assertNull(fallo.codigo());
        assertNull(fallo.prospectoId());
        assertEquals("Meta no pudo entregar el mensaje.", fallo.causa());
    }

    @Test
    void exponeElProspectoYLaPlantillaParaUbicarElFallo() {
        devuelve(mensaje("131042", "detalle"));

        var fallo = service.recentFailures().getFirst();

        assertEquals(11L, fallo.prospectoId());
        assertEquals("Carlos Flores", fallo.prospectoNombre());
        assertEquals("seguimiento_prospecto", fallo.plantillaNombre());
        assertEquals("template", fallo.tipoMensaje());
    }

    private CrmWhatsappMessage mensaje(String codigo, String detalle) {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(11L);
        prospecto.setNombre("Carlos Flores");

        CrmWhatsappMessage mensaje = new CrmWhatsappMessage();
        mensaje.setId(29L);
        mensaje.setProspecto(prospecto);
        mensaje.setTipoMensaje("template");
        mensaje.setPlantillaNombre("seguimiento_prospecto");
        mensaje.setEstado("FALLIDO");
        mensaje.setDireccion("SALIENTE");
        mensaje.setErrorCodigo(codigo);
        mensaje.setErrorDetalle(detalle);
        mensaje.setMensajeEn(OffsetDateTime.now());
        return mensaje;
    }
}
