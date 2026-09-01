package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.ScheduleQuoteReengagementRequest;
import com.azurion.saascore.crm.domain.WhatsappTemplate;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappReengagementOutbox;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsappReengagementServiceTest {

    @Mock
    private CrmWhatsappReengagementOutboxRepository outboxRepository;
    @Mock
    private CrmProspectoRepository prospectoRepository;
    @Mock
    private WhatsappIntegrationService whatsappIntegrationService;
    @Mock
    private CotizacionRepository cotizacionRepository;

    private WhatsappReengagementService service() {
        return new WhatsappReengagementService(
                outboxRepository,
                prospectoRepository,
                whatsappIntegrationService,
                cotizacionRepository,
                new CotizacionReengagementParameterBuilder(),
                new ObjectMapper()
        );
    }

    @Test
    void tomaLaUltimaCotizacionQueElClienteVioYLlenaLasVariables() {
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto()));
        when(cotizacionRepository.findAllByCrmProspectoId(11L)).thenReturn(List.of(
                cotizacion(9000L, "BORRADOR"),
                cotizacion(8421L, "ENVIADA"),
                cotizacion(7000L, "ENVIADA")
        ));
        when(whatsappIntegrationService.requireSendableTemplate("seguimiento", "es_PE"))
                .thenReturn(plantillaDeCincoVariables());
        when(outboxRepository.findByTenantIdAndDedupeKey(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().scheduleFromQuote(11L, request(null));

        // La 9000 es BORRADOR: el cliente nunca la vio, se salta.
        assertEquals(
                List.of("Carlos", "8421", "Curso de Python Intermedio", "S/ 1200.00", "15/09/2026"),
                response.parametros()
        );

        ArgumentCaptor<CrmWhatsappReengagementOutbox> job =
                ArgumentCaptor.forClass(CrmWhatsappReengagementOutbox.class);
        verify(outboxRepository).save(job.capture());
        assertEquals("PENDING", job.getValue().getStatus());
        assertEquals(11L, job.getValue().getProspectoId());
    }

    @Test
    void rechazaCitarUnaCotizacionEnBorrador() {
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto()));
        when(cotizacionRepository.findByIdAndCrmProspectoId(9000L, 11L))
                .thenReturn(Optional.of(cotizacion(9000L, "BORRADOR")));

        BusinessException error = assertThrows(BusinessException.class, () ->
                service().scheduleFromQuote(11L, request(9000L)));

        assertEquals("CRM_COTIZACION_NO_CITABLE", error.getCode());
        assertTrue(error.getMessage().contains("BORRADOR"));
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void rechazaUnaCotizacionQueNoEsDelProspecto() {
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto()));
        when(cotizacionRepository.findByIdAndCrmProspectoId(9999L, 11L)).thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class, () ->
                service().scheduleFromQuote(11L, request(9999L)));

        assertEquals("CRM_COTIZACION_NO_ENCONTRADA", error.getCode());
    }

    @Test
    void avisaCuandoNoHayNingunaCotizacionCitable() {
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto()));
        when(cotizacionRepository.findAllByCrmProspectoId(11L))
                .thenReturn(List.of(cotizacion(9000L, "BORRADOR")));

        BusinessException error = assertThrows(BusinessException.class, () ->
                service().scheduleFromQuote(11L, request(null)));

        assertEquals("CRM_COTIZACION_NO_ENCONTRADA", error.getCode());
    }

    @Test
    void noProgramaNadaAUnProspectoQueSeDioDeBaja() {
        CrmProspecto prospecto = prospecto();
        prospecto.setWhatsappOptoutEn(java.time.OffsetDateTime.now());
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto));
        when(cotizacionRepository.findAllByCrmProspectoId(11L))
                .thenReturn(List.of(cotizacion(8421L, "ENVIADA")));

        BusinessException error = assertThrows(BusinessException.class, () ->
                service().scheduleFromQuote(11L, request(null)));

        assertEquals("CRM_WHATSAPP_PROSPECTO_DIO_DE_BAJA", error.getCode());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void rechazaUnaFechaDemasiadoCercana() {
        when(prospectoRepository.findById(11L)).thenReturn(Optional.of(prospecto()));
        when(cotizacionRepository.findAllByCrmProspectoId(11L))
                .thenReturn(List.of(cotizacion(8421L, "ENVIADA")));

        // La fecha se valida antes de consultar el catalogo del WABA.
        BusinessException error = assertThrows(BusinessException.class, () ->
                service().scheduleFromQuote(11L, new ScheduleQuoteReengagementRequest(
                        "seguimiento", "es_PE", LocalDateTime.now().plusMinutes(1), null, null)));

        assertEquals("CRM_WHATSAPP_REENGANCHE_FECHA_INVALIDA", error.getCode());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void cancelaLosPendientesDeUnProspecto() {
        when(outboxRepository.cancelPendingForProspecto(anyString(), anyLong(), anyString(), any()))
                .thenReturn(2);

        assertEquals(2, service().cancelForProspecto(11L, "motivo"));
    }

    private ScheduleQuoteReengagementRequest request(Long cotizacionId) {
        return new ScheduleQuoteReengagementRequest(
                "seguimiento",
                "es_PE",
                LocalDateTime.now().plusDays(7),
                cotizacionId,
                null
        );
    }

    private WhatsappTemplate plantillaDeCincoVariables() {
        return new WhatsappTemplate(
                "t-1",
                "seguimiento",
                "es_PE",
                "APPROVED",
                "UTILITY",
                List.of(new WhatsappTemplate.Component(
                        "BODY",
                        "Hola {{1}}, tu cotizacion #{{2}} por {{3}} suma {{4}} y vence el {{5}}.",
                        List.of("1", "2", "3", "4", "5")
                )),
                null
        );
    }

    private CrmProspecto prospecto() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(11L);
        prospecto.setNombre("Carlos Flores Rojas");
        prospecto.setInteresPrincipal("Curso de Python Basico");
        return prospecto;
    }

    private Cotizacion cotizacion(Long id, String estado) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(id);
        cotizacion.setEstado(estado);
        cotizacion.setMoneda("PEN");
        cotizacion.setTotal(new java.math.BigDecimal("1200"));
        cotizacion.setFechaVencimiento(java.time.LocalDate.of(2026, 9, 15));

        var detalle = new com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle();
        detalle.setCatalogoNombre("Curso de Python Intermedio");
        cotizacion.setDetalles(new java.util.ArrayList<>(List.of(detalle)));
        return cotizacion;
    }
}
