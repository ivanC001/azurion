package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.CrmWhatsappTemplateResponse;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsappReengagementGuideServiceTest {

    @Mock
    private WhatsappIntegrationService whatsappIntegrationService;

    @InjectMocks
    private WhatsappReengagementGuideService service;

    @Test
    void guiaAlaConfiguracionCuandoWhatsappNoEstaConectado() {
        when(whatsappIntegrationService.listApprovedTemplates())
                .thenThrow(new BusinessException("CRM_WHATSAPP_NO_CONFIGURADO", "no configurado"));

        var guia = service.guide();

        assertFalse(guia.listoParaProgramar());
        assertTrue(guia.resumen().contains("no esta configurado"));
        assertEquals(1, guia.pasos().size());
        assertTrue(guia.pasos().getFirst().contains("Access token"));
        assertTrue(guia.advertencias().isEmpty());
    }

    @Test
    void noRepiteElTextoDeMetaCuandoElFalloNoEsDeConfiguracion() {
        when(whatsappIntegrationService.listApprovedTemplates()).thenThrow(
                new BusinessException("CRM_WHATSAPP_NO_DISPONIBLE", "detalle interno de Graph"));

        var guia = service.guide();

        assertFalse(guia.resumen().contains("detalle interno"));
        assertTrue(guia.resumen().contains("No se pudo consultar"));
    }

    @Test
    void avisaQueLaPlantillaDeEjemploNoSirveYNoLaCuentaComoUtilizable() {
        when(whatsappIntegrationService.listApprovedTemplates())
                .thenReturn(List.of(plantilla("hello_world", "UTILITY", true, null)));

        var guia = service.guide();

        assertFalse(guia.listoParaProgramar());
        assertTrue(guia.plantillasUtilizables().isEmpty());
        assertTrue(guia.advertencias().stream()
                .anyMatch(aviso -> aviso.contains("hello_world") && aviso.contains("numeros de prueba")));
    }

    @Test
    void explicaPorQueUnaPlantillaAprobadaNoSePuedeEnviar() {
        when(whatsappIntegrationService.listApprovedTemplates()).thenReturn(List.of(
                plantilla("con_imagen", "UTILITY", false, "Usa un encabezado de imagen.")
        ));

        var guia = service.guide();

        assertFalse(guia.listoParaProgramar());
        assertTrue(guia.advertencias().stream()
                .anyMatch(aviso -> aviso.contains("con_imagen") && aviso.contains("encabezado de imagen")));
    }

    @Test
    void avisaCuandoTodoLoUtilizableEsMarketing() {
        when(whatsappIntegrationService.listApprovedTemplates())
                .thenReturn(List.of(plantilla("seguimiento", "MARKETING", true, null)));

        var guia = service.guide();

        assertTrue(guia.listoParaProgramar());
        assertEquals(1, guia.plantillasUtilizables().size());
        assertTrue(guia.advertencias().stream().anyMatch(aviso -> aviso.contains("marketing")));
    }

    @Test
    void noAvisaNadaCuandoYaHayUnaPlantillaUtilityLista() {
        when(whatsappIntegrationService.listApprovedTemplates())
                .thenReturn(List.of(plantilla("seguimiento_cotizacion", "UTILITY", true, null)));

        var guia = service.guide();

        assertTrue(guia.listoParaProgramar());
        assertTrue(guia.advertencias().isEmpty());
        assertTrue(guia.resumen().contains("Listo"));
        // Sin plantillas pendientes, la guia solo deja los pasos de uso.
        assertEquals(2, guia.pasos().size());
    }

    @Test
    void siempreDevuelveElModeloDePlantillaParaCopiar() {
        when(whatsappIntegrationService.listApprovedTemplates()).thenReturn(List.of());

        var modelo = service.guide().plantillaSugerida();

        assertEquals("UTILITY", modelo.categoria());
        assertEquals(5, modelo.variables().size());
        assertEquals(2, modelo.botones().size());
        assertTrue(modelo.cuerpo().contains("{{1}}"));
        assertTrue(modelo.cuerpo().contains("{{5}}"));
    }

    private CrmWhatsappTemplateResponse plantilla(
            String nombre, String categoria, boolean disponible, String motivo) {
        return new CrmWhatsappTemplateResponse(
                nombre, "es_PE", categoria, "cuerpo", 5, "t-1", "APPROVED",
                disponible, motivo, List.of()
        );
    }
}
