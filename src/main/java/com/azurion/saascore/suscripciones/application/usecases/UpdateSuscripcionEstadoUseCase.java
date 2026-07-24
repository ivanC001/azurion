package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.dto.UpdateSuscripcionEstadoRequest;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSuscripcionEstadoUseCase {

    private static final Set<String> ESTADOS_VALIDOS =
            Set.of("ACTIVA", "SUSPENDIDA", "CANCELADA");

    private final SuscripcionRepository suscripcionRepository;

    @Transactional
    public SuscripcionResponse execute(Long id, UpdateSuscripcionEstadoRequest request) {
        Suscripcion suscripcion = suscripcionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUSCRIPCION_NOT_FOUND", "Suscripcion not found: " + id));

        String estado = request.estado().trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BusinessException(
                    "ESTADO_SUSCRIPCION_INVALIDO",
                    "El estado debe ser ACTIVA, SUSPENDIDA o CANCELADA"
            );
        }
        if ("ACTIVA".equals(estado)
                && !"ACTIVO".equalsIgnoreCase(suscripcion.getPlan().getEstado())) {
            throw new BusinessException(
                    "PLAN_NO_DISPONIBLE",
                    "No se puede activar una suscripcion con un plan inactivo"
            );
        }
        if ("ACTIVA".equals(estado)) {
            suscripcionRepository.findAllActiveStateForUpdate(
                    suscripcion.getEmpresa().getId()
            ).stream()
                    .filter(item -> !item.getId().equals(suscripcion.getId()))
                    .forEach(item -> {
                        item.setEstado("SUSPENDIDA");
                        item.setFechaFin(LocalDate.now());
                        suscripcionRepository.save(item);
                    });
        }

        suscripcion.setEstado(estado);
        suscripcion.setFechaFin(switch (estado) {
            case "ACTIVA" -> null;
            case "CANCELADA" -> request.fechaFin() == null
                    ? LocalDate.now()
                    : request.fechaFin();
            default -> request.fechaFin();
        });
        return SuscripcionMapper.toResponse(suscripcionRepository.save(suscripcion));
    }
}
