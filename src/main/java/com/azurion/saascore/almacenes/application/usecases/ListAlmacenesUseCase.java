package com.azurion.saascore.almacenes.application.usecases;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAlmacenesUseCase {

    private final AlmacenRepository almacenRepository;

    @Transactional(readOnly = true)
    public List<AlmacenResponse> execute() {
        return almacenRepository.findAll().stream()
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
    }
}
