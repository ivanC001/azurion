package com.azurion.saascore.crm.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.azurion.saascore.crm.application.dto.CrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.mappers.CrmMapper;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CrmMapperTest {

    @Test
    void mapsCatalogUsageCountersWithoutLosingCommercialData() {
        CrmCatalogoItem item = new CrmCatalogoItem();
        item.setId(12L);
        item.setTipoItem("CURSO");
        item.setNombre("Python intermedio");
        item.setDescripcion("Curso comercial");
        item.setPrecioReferencial(new BigDecimal("450.00"));
        item.setEstado("ACTIVO");
        item.setMetadataJson("{\"attributes\":{\"nivel\":\"Intermedio\"}}");
        item.setPublicToken("public-token");
        item.setPublicEnabled(true);
        item.setLandingSlug("python-intermedio");
        item.setCreatedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 7, 23, 11, 0));

        CrmCatalogoItemResponse response = CrmMapper.toCatalogoItemResponse(item, 8, 3, 2);

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.nombre()).isEqualTo("Python intermedio");
        assertThat(response.precioReferencial()).isEqualByComparingTo("450.00");
        assertThat(response.prospectosCount()).isEqualTo(8);
        assertThat(response.oportunidadesCount()).isEqualTo(3);
        assertThat(response.landingsCount()).isEqualTo(2);
    }
}
