package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.application.services.CrmCotizacionIntegrationService;
import com.azurion.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCotizacionEstadoUseCase {

    private static final Set<String> ESTADOS = Set.of(
            "BORRADOR", "ENVIADA", "EN_SEGUIMIENTO", "ACEPTADA", "RECHAZADA", "NEGOCIACION", "VENCIDA"
    );
    private static final Set<String> DECISIONES = Set.of("NEGOCIACION", "VENTA");

    private final CotizacionRepository cotizacionRepository;
    private final GetCotizacionUseCase getCotizacionUseCase;
    private final CrmCotizacionIntegrationService crmCotizacionIntegrationService;

    @Transactional
    public CotizacionResponse execute(Long id, UpdateCotizacionEstadoRequest request) {
        Cotizacion cotizacion = getCotizacionUseCase.find(id);
        if ("CONVERTIDA".equals(cotizacion.getEstado())) {
            throw new BusinessException("COTIZACION_YA_CONVERTIDA", "Una cotizacion convertida no puede cambiar de estado");
        }
        String estado = normalizeEstado(request.estado());
        applyFlowData(cotizacion, estado, request);
        cotizacion.setEstado(estado);
        Cotizacion saved = cotizacionRepository.save(cotizacion);
        crmCotizacionIntegrationService.onCotizacionEstado(saved.getCrmOportunidadId(), estado, saved.getTotal(), saved.getDecisionSiguiente());
        return CotizacionMapper.toResponse(saved);
    }

    private void applyFlowData(Cotizacion cotizacion, String estado, UpdateCotizacionEstadoRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        if ("ENVIADA".equals(estado)) {
            if (cotizacion.getFechaEnvio() == null) {
                cotizacion.setFechaEnvio(now);
            }
            cotizacion.setCanalEnvio(normalizeText(request.canalEnvio(), "MANUAL"));
        } else if ("EN_SEGUIMIENTO".equals(estado)) {
            if (cotizacion.getFechaEnvio() == null) {
                cotizacion.setFechaEnvio(now);
            }
            cotizacion.setCanalEnvio(normalizeText(request.canalEnvio(), cotizacion.getCanalEnvio() == null ? "MANUAL" : cotizacion.getCanalEnvio()));
            cotizacion.setProximoSeguimientoEn(request.proximoSeguimientoEn() == null ? now.plusDays(1) : request.proximoSeguimientoEn());
        } else if ("ACEPTADA".equals(estado)) {
            cotizacion.setFechaRespuesta(now);
            cotizacion.setDecisionSiguiente(normalizeDecision(request.decisionSiguiente(), "VENTA"));
            cotizacion.setMotivoRechazo(null);
        } else if ("RECHAZADA".equals(estado)) {
            cotizacion.setFechaRespuesta(now);
            cotizacion.setMotivoRechazo(trim(request.motivoRechazo()));
            cotizacion.setDecisionSiguiente(null);
        } else if ("NEGOCIACION".equals(estado)) {
            cotizacion.setDecisionSiguiente("NEGOCIACION");
        }
    }

    private String normalizeEstado(String value) {
        String estado = value == null ? "" : value.trim().toUpperCase();
        if (!ESTADOS.contains(estado)) {
            throw new BusinessException("COTIZACION_ESTADO_INVALIDO", "Estado de cotizacion invalido");
        }
        return estado;
    }

    private String normalizeDecision(String value, String defaultValue) {
        String decision = value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
        if (!DECISIONES.contains(decision)) {
            throw new BusinessException("COTIZACION_DECISION_INVALIDA", "Decision siguiente invalida");
        }
        return decision;
    }

    private String normalizeText(String value, String defaultValue) {
        String resolved = value == null || value.isBlank() ? defaultValue : value.trim();
        return resolved == null ? null : resolved.toUpperCase();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
