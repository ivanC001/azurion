package com.azurion.saascore.inventory.application.usecases;

import com.azurion.saascore.inventory.application.dto.CategoriaResponse;
import com.azurion.saascore.inventory.domain.repositories.CategoriaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCategoriasUseCase {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> execute() {
        return categoriaRepository.findByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO").stream()
                .map(categoria -> new CategoriaResponse(
                        categoria.getId(),
                        categoria.getNombre(),
                        categoria.getDescripcion(),
                        categoria.getPadre() == null ? null : categoria.getPadre().getId(),
                        categoria.getEstado()
                ))
                .toList();
    }
}
