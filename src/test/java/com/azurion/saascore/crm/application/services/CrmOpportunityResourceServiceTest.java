package com.azurion.saascore.crm.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.crm.application.dto.CrmOportunidadRecursoRequest;
import com.azurion.saascore.crm.application.support.CrmCurrencyConverter;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmOportunidadRecurso;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRecursoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.infrastructure.storage.CrmPrivateFileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CrmOpportunityResourceServiceTest {

    @Mock
    private CrmOportunidadRepository oportunidadRepository;
    @Mock
    private CrmOportunidadRecursoRepository recursoRepository;
    @Mock
    private CrmCatalogoItemRepository catalogoItemRepository;
    @Mock
    private CrmCurrencyConverter currencyConverter;
    @Mock
    private CrmPrivateFileStorageService fileStorageService;
    @Mock
    private AuthorizationService authorizationService;

    private CrmOpportunityResourceService service;

    @BeforeEach
    void setUp() {
        service = new CrmOpportunityResourceService(
                oportunidadRepository,
                recursoRepository,
                catalogoItemRepository,
                currencyConverter,
                fileStorageService,
                authorizationService,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addingARequirementRecalculatesTheEstimateInTheOpportunityCurrency() {
        CrmOportunidad opportunity = opportunity("USD", "1100.00");

        when(authorizationService.currentUsuarioId()).thenReturn(7L);
        when(oportunidadRepository.findWithRelationsById(31L)).thenReturn(Optional.of(opportunity));
        when(recursoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recursoRepository.findByOportunidadIdOrderByCreatedAtDescIdDesc(31L)).thenReturn(List.of(
                requirement(opportunity, 24L, "Toyota Hilux", "1", "40000.00"),
                requirement(opportunity, 12L, "Curso Python", "1", "1100.00")
        ));

        CrmCatalogoItem curso = new CrmCatalogoItem();
        curso.setMoneda("PEN");
        CrmCatalogoItem hilux = new CrmCatalogoItem();
        hilux.setMoneda("USD");
        when(catalogoItemRepository.findById(12L)).thenReturn(Optional.of(curso));
        when(catalogoItemRepository.findById(24L)).thenReturn(Optional.of(hilux));
        when(currencyConverter.normalizeCurrency("USD")).thenReturn("USD");
        when(currencyConverter.convert(any(BigDecimal.class), eq("USD"), eq("USD")))
                .thenReturn(new BigDecimal("40000.00"));
        when(currencyConverter.convert(any(BigDecimal.class), eq("PEN"), eq("USD")))
                .thenReturn(new BigDecimal("300.00"));

        service.create(31L, new CrmOportunidadRecursoRequest("REQUISITO", Map.of(
                "nombre", "Toyota Hilux",
                "cantidad", 1,
                "precioUnitario", 40000
        )), null);

        assertThat(opportunity.getMontoEstimado()).isEqualByComparingTo("40300.00");
        verify(oportunidadRepository).save(opportunity);
    }

    @Test
    void deletingTheLastRequirementKeepsThePreviousEstimate() {
        CrmOportunidad opportunity = opportunity("PEN", "1100.00");
        CrmOportunidadRecurso resource = requirement(opportunity, null, "Curso Python", "1", "1100.00");

        when(authorizationService.currentUsuarioId()).thenReturn(7L);
        when(recursoRepository.findWithOportunidadById(90L)).thenReturn(Optional.of(resource));
        when(recursoRepository.findByOportunidadIdOrderByCreatedAtDescIdDesc(31L)).thenReturn(List.of());

        service.delete(31L, 90L);

        assertThat(opportunity.getMontoEstimado()).isEqualByComparingTo("1100.00");
        verify(oportunidadRepository, never()).save(any());
    }

    private CrmOportunidad opportunity(String moneda, String montoEstimado) {
        CrmOportunidad opportunity = new CrmOportunidad();
        opportunity.setId(31L);
        opportunity.setResponsableId("7");
        opportunity.setMoneda(moneda);
        opportunity.setMontoEstimado(new BigDecimal(montoEstimado));
        return opportunity;
    }

    private CrmOportunidadRecurso requirement(CrmOportunidad opportunity, Long catalogoItemId,
                                              String nombre, String cantidad, String precio) {
        CrmOportunidadRecurso resource = new CrmOportunidadRecurso();
        resource.setOportunidad(opportunity);
        resource.setTipo("REQUISITO");
        String catalogPart = catalogoItemId == null ? "" : "\"catalogoItemId\":" + catalogoItemId + ",";
        resource.setDataJson("{" + catalogPart + "\"nombre\":\"" + nombre
                + "\",\"cantidad\":" + cantidad + ",\"precioUnitario\":" + precio + "}");
        return resource;
    }
}
