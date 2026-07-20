package com.azurion.saascore.almacenes.application.usecases;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.shared.api.PageRequestSupport;
import com.azurion.shared.api.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class ListAlmacenesUseCase {

    private final AlmacenRepository almacenRepository;

    @Transactional(readOnly = true)
    public List<AlmacenResponse> execute() {
        return page(0, PageRequestSupport.MAX_SIZE).content();
    }

    @Transactional(readOnly = true)
    public PageResponse<AlmacenResponse> page(int page, int size) {
        var result = almacenRepository.findAll(PageRequestSupport.of(page, size, Sort.by("nombre").ascending()));
        List<AlmacenResponse> content = result.getContent().stream()
                .map(a -> new AlmacenResponse(
                        a.getId(),
                        a.getCodigo(),
                        a.getNombre(),
                        a.getDireccion(),
                        a.getSucursal().getId(),
                        a.getSucursal().getCodigo(),
                        a.getSucursal().getNombre(),
                        a.getTipoAlmacen(),
                        a.getEstado(),
                        a.isActivo()
                ))
                .toList();
        return PageResponse.from(result, content);
    }
}
