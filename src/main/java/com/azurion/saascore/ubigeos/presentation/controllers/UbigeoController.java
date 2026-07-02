package com.azurion.saascore.ubigeos.presentation.controllers;

import com.azurion.saascore.ubigeos.application.dto.UbigeoResponse;
import com.azurion.saascore.ubigeos.application.usecases.SearchUbigeosUseCase;
import com.azurion.shared.api.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/saas/ubigeos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ADMIN_EMPRESA','SALES')")
public class UbigeoController {

    private final SearchUbigeosUseCase searchUbigeosUseCase;

    @GetMapping
    public ApiResponse<List<UbigeoResponse>> search(@RequestParam(required = false) String query) {
        return ApiResponse.ok(searchUbigeosUseCase.execute(query), "Ubigeos");
    }
}
