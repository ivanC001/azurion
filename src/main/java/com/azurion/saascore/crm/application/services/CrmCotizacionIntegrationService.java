package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmOportunidadHistorial;
import com.azurion.saascore.crm.domain.repositories.CrmEtapaPipelineRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadHistorialRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrmCotizacionIntegrationService {

    private final CrmOportunidadRepository oportunidadRepository;
    private final CrmEtapaPipelineRepository etapaRepository;
    private final CrmOportunidadHistorialRepository historialRepository;

    public void onCotizacionEstado(Long oportunidadId, String estado, BigDecimal monto, String decisionSiguiente) {
        if (oportunidadId == null || estado == null) {
            return;
        }
        if ("ENVIADA".equals(estado) || "EN_SEGUIMIENTO".equals(estado)) {
            move(oportunidadId, "COTIZADO", "Cotizacion enviada a seguimiento", null);
        } else if ("ACEPTADA".equals(estado)) {
            if ("VENTA".equals(decisionSiguiente)) {
                move(oportunidadId, "GANADO", "Cotizacion aceptada para venta", monto);
            } else {
                move(oportunidadId, "NEGOCIACION", "Cotizacion aceptada para negociacion", monto);
            }
        } else if ("NEGOCIACION".equals(estado)) {
            move(oportunidadId, "NEGOCIACION", "Cotizacion enviada a negociacion", monto);
        } else if ("RECHAZADA".equals(estado)) {
            appendOnly(oportunidadId, "Cotizacion rechazada. La oportunidad queda abierta para nueva propuesta");
        }
    }

    public void onCotizacionConvertidaVenta(Long oportunidadId, BigDecimal monto) {
        if (oportunidadId == null) {
            return;
        }
        move(oportunidadId, "GANADO", "Cotizacion convertida en venta", monto);
    }

    private void move(Long oportunidadId, String etapaCodigo, String observacion, BigDecimal montoReal) {
        oportunidadRepository.findById(oportunidadId).ifPresent(oportunidad -> etapaRepository.findByCodigo(etapaCodigo)
                .filter(CrmEtapaPipeline::isActivo)
                .ifPresent(etapa -> apply(oportunidad, etapa, observacion, montoReal)));
    }

    private void appendOnly(Long oportunidadId, String observacion) {
        oportunidadRepository.findById(oportunidadId).ifPresent(oportunidad -> {
            CrmEtapaPipeline etapaActual = oportunidad.getEtapaPipeline();
            if (etapaActual == null) {
                return;
            }
            CrmOportunidadHistorial historial = new CrmOportunidadHistorial();
            historial.setOportunidad(oportunidad);
            historial.setEtapaOrigen(etapaActual);
            historial.setEtapaDestino(etapaActual);
            historial.setUsuarioId("cotizaciones");
            historial.setObservacion(observacion);
            historial.setFechaCambio(OffsetDateTime.now());
            historialRepository.save(historial);
        });
    }

    private void apply(CrmOportunidad oportunidad, CrmEtapaPipeline destino, String observacion, BigDecimal montoReal) {
        CrmEtapaPipeline origen = oportunidad.getEtapaPipeline();
        if (origen != null && origen.getId().equals(destino.getId())) {
            if (montoReal != null) {
                oportunidad.setMontoReal(montoReal);
                oportunidadRepository.save(oportunidad);
            }
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        oportunidad.setEtapaPipeline(destino);
        oportunidad.setEtapa(destino.getCodigo());
        oportunidad.setFechaUltimaActualizacion(now);
        if (destino.isGanado()) {
            oportunidad.setEstado("GANADA");
            oportunidad.setProbabilidad(100);
            oportunidad.setFechaGanada(now);
            oportunidad.setFechaCierreReal(now);
            oportunidad.setMontoReal(montoReal == null ? oportunidad.getMontoEstimado() : montoReal);
            oportunidad.setMotivoPerdida(null);
        } else if (destino.isPerdido()) {
            oportunidad.setEstado("PERDIDA");
            oportunidad.setProbabilidad(0);
            oportunidad.setFechaPerdida(now);
            oportunidad.setFechaCierreReal(now);
            oportunidad.setMotivoPerdida(observacion);
        }
        oportunidadRepository.save(oportunidad);

        CrmOportunidadHistorial historial = new CrmOportunidadHistorial();
        historial.setOportunidad(oportunidad);
        historial.setEtapaOrigen(origen);
        historial.setEtapaDestino(destino);
        historial.setUsuarioId("cotizaciones");
        historial.setObservacion(observacion);
        historial.setFechaCambio(now);
        historialRepository.save(historial);
    }
}
