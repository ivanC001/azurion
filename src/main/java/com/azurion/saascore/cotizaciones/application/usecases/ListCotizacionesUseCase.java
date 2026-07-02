package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCotizacionesUseCase {

    private final CotizacionRepository cotizacionRepository;

    @Transactional(readOnly = true)
    public List<CotizacionResponse> execute() {
        return CotizacionMapper.toResponses(cotizacionRepository.findAllByOrderByFechaEmisionDescIdDesc());
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponse> execute(Long crmOportunidadId) {
        if (crmOportunidadId == null) {
            return execute();
        }
        return CotizacionMapper.toResponses(cotizacionRepository.findByCrmOportunidadIdOrderByFechaEmisionDescIdDesc(crmOportunidadId));
    }
}
