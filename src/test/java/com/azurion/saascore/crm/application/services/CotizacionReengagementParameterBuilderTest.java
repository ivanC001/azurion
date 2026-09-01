package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azurion.saascore.crm.application.dto.CotizacionReengagementField;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CotizacionReengagementParameterBuilderTest {

    private final CotizacionReengagementParameterBuilder builder =
            new CotizacionReengagementParameterBuilder();

    @Test
    void armaLosCincoCamposPorDefectoEnOrden() {
        List<String> parametros = builder.build(prospecto(), cotizacion(), null);

        assertEquals(
                List.of("Carlos", "8421", "Curso de Python Intermedio", "S/ 1200.00", "15/09/2026"),
                parametros
        );
    }

    @Test
    void respetaElOrdenPedidoYSoportaSubconjuntos() {
        List<String> parametros = builder.build(prospecto(), cotizacion(), List.of(
                CotizacionReengagementField.VENCIMIENTO,
                CotizacionReengagementField.NOMBRE_COMPLETO,
                CotizacionReengagementField.ASESOR
        ));

        assertEquals(List.of("15/09/2026", "Carlos Flores Rojas", "Ana Perez Diaz"), parametros);
    }

    @Test
    void usaElInteresDelProspectoCuandoLaCotizacionNoTieneItems() {
        Cotizacion cotizacion = cotizacion();
        cotizacion.setDetalles(List.of());

        List<String> parametros = builder.build(prospecto(), cotizacion,
                List.of(CotizacionReengagementField.PRODUCTO));

        assertEquals(List.of("Curso de Python Basico"), parametros);
    }

    @Test
    void dejaLosValoresEnUnaSolaLinea() {
        Cotizacion cotizacion = cotizacion();
        cotizacion.getDetalles().getFirst().setCatalogoNombre("Curso  de\nPython\tIntermedio");

        List<String> parametros = builder.build(prospecto(), cotizacion,
                List.of(CotizacionReengagementField.PRODUCTO));

        assertEquals(List.of("Curso de Python Intermedio"), parametros);
    }

    @Test
    void avisaCualCampoFaltaEnLugarDeMandarUnMensajeRoto() {
        Cotizacion cotizacion = cotizacion();
        cotizacion.setFechaVencimiento(null);

        BusinessException error = assertThrows(BusinessException.class, () ->
                builder.build(prospecto(), cotizacion, List.of(CotizacionReengagementField.VENCIMIENTO)));

        assertEquals("CRM_WHATSAPP_REENGANCHE_DATO_FALTANTE", error.getCode());
        assertTrue(error.getMessage().contains("VENCIMIENTO"));
    }

    @Test
    void usaElCodigoDeMonedaCuandoNoHaySimboloConocido() {
        Cotizacion cotizacion = cotizacion();
        cotizacion.setMoneda("COP");
        cotizacion.setTotal(new BigDecimal("980000"));

        List<String> parametros = builder.build(prospecto(), cotizacion,
                List.of(CotizacionReengagementField.TOTAL));

        assertEquals(List.of("COP 980000.00"), parametros);
    }

    private CrmProspecto prospecto() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(11L);
        prospecto.setNombre("Carlos Flores Rojas");
        prospecto.setInteresPrincipal("Curso de Python Basico");
        return prospecto;
    }

    private Cotizacion cotizacion() {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(8421L);
        cotizacion.setEstado("ENVIADA");
        cotizacion.setMoneda("PEN");
        cotizacion.setTotal(new BigDecimal("1200"));
        cotizacion.setFechaVencimiento(LocalDate.of(2026, 9, 15));
        cotizacion.setUsuarioNombre("Ana");
        cotizacion.setAsesorApellidos("Perez Diaz");

        CotizacionDetalle detalle = new CotizacionDetalle();
        detalle.setCatalogoNombre("Curso de Python Intermedio");
        cotizacion.setDetalles(new java.util.ArrayList<>(List.of(detalle)));
        return cotizacion;
    }
}
