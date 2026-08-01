package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.mappers.CotizacionMapper;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class ListCotizacionesUseCase {

    private static final int LEGACY_MAX_SIZE = 200;

    private final CotizacionRepository cotizacionRepository;

    @Transactional(readOnly = true)
    public List<CotizacionResponse> execute() {
        List<Long> ids = cotizacionRepository.findRecentIds(PageRequest.of(0, LEGACY_MAX_SIZE)).getContent();
        return ids.isEmpty()
                ? List.of()
                : CotizacionMapper.toResponses(cotizacionRepository.findDetailedByIdIn(ids));
    }

    @Transactional(readOnly = true)
    public List<CotizacionResponse> execute(Long crmOportunidadId) {
        if (crmOportunidadId == null) {
            return execute();
        }
        return CotizacionMapper.toResponses(cotizacionRepository.findByCrmOportunidadIdOrderByFechaEmisionDescIdDesc(crmOportunidadId));
    }
}
