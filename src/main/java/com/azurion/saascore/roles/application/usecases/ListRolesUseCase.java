package com.azurion.saascore.roles.application.usecases;

import com.azurion.saascore.roles.application.dto.RolResponse;
import com.azurion.saascore.roles.application.mappers.RolesMapper;
import com.azurion.saascore.roles.domain.repositories.RolRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListRolesUseCase {

    private final RolRepository rolRepository;

    public List<RolResponse> execute() {
        return rolRepository.findAllByOrderByNombreAsc().stream()
                .map(RolesMapper::toRolResponse)
                .toList();
    }
}
