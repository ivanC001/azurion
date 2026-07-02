package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.cotizaciones.application.dto.CreatePromocionCotizacionRequest;
import com.azurion.saascore.cotizaciones.application.dto.PromocionCotizacionResponse;
import com.azurion.saascore.cotizaciones.domain.entities.PromocionCotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.PromocionCotizacionRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromocionCotizacionUseCase {

    private static final Set<String> TIPOS = Set.of("MONTO", "PORCENTAJE");
    private static final Set<String> ESTADOS = Set.of("ACTIVA", "INACTIVA");

    private final PromocionCotizacionRepository repository;

    @Transactional(readOnly = true)
    public List<PromocionCotizacionResponse> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PromocionCotizacionResponse create(CreatePromocionCotizacionRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (repository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("PROMOCION_COTIZACION_DUPLICADA", "Ya existe una promocion con ese codigo");
        }

        PromocionCotizacion promocion = new PromocionCotizacion();
        promocion.setCodigo(codigo);
        promocion.setNombre(trimRequired(request.nombre(), "El nombre de la promocion es obligatorio"));
        promocion.setDescripcion(trim(request.descripcion()));
        promocion.setTipoDescuento(normalizeTipo(request.tipoDescuento()));
        promocion.setValor(request.valor());
        promocion.setFechaInicio(request.fechaInicio());
        promocion.setFechaFin(request.fechaFin());
        promocion.setEstado(normalizeEstado(request.estado()));
        return toResponse(repository.save(promocion));
    }

    private PromocionCotizacionResponse toResponse(PromocionCotizacion promocion) {
        return new PromocionCotizacionResponse(
                promocion.getId(),
                promocion.getCodigo(),
                promocion.getNombre(),
                promocion.getDescripcion(),
                promocion.getTipoDescuento(),
                promocion.getValor(),
                promocion.getFechaInicio(),
                promocion.getFechaFin(),
                promocion.getEstado()
        );
    }

    private String normalizeTipo(String value) {
        String tipo = normalize(value);
        if (!TIPOS.contains(tipo)) {
            throw new BusinessException("PROMOCION_COTIZACION_TIPO_INVALIDO", "Tipo de descuento invalido");
        }
        return tipo;
    }

    private String normalizeEstado(String value) {
        String estado = value == null || value.isBlank() ? "ACTIVA" : normalize(value);
        if (!ESTADOS.contains(estado)) {
            throw new BusinessException("PROMOCION_COTIZACION_ESTADO_INVALIDO", "Estado de promocion invalido");
        }
        return estado;
    }

    private String normalizeCode(String value) {
        return trimRequired(value, "El codigo de promocion es obligatorio").replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return trimRequired(value, "Valor obligatorio").toUpperCase(Locale.ROOT);
    }

    private String trimRequired(String value, String message) {
        String resolved = trim(value);
        if (resolved == null) {
            throw new BusinessException("PROMOCION_COTIZACION_VALOR_REQUERIDO", message);
        }
        return resolved;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
