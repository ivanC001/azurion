package com.azurion;

import static org.assertj.core.api.Assertions.assertThat;

import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AzurionApplicationTests {

    @Autowired
    private CrmCatalogoItemRepository catalogoItemRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void catalogUsageSummaryQueryIsCompatibleWithTheTenantSchema() {
        CrmCatalogoItem item = new CrmCatalogoItem();
        item.setTipoItem("CURSO");
        item.setNombre("Oferta de prueba");
        item.setPrecioReferencial(BigDecimal.TEN);
        item.setEstado("ACTIVO");
        item.setPublicToken("test-public-token");
        item.setPublicEnabled(true);
        catalogoItemRepository.saveAndFlush(item);

        assertThat(catalogoItemRepository.summarizeUsage())
                .singleElement()
                .satisfies(usage -> {
                    assertThat(usage.getCatalogoItemId()).isEqualTo(item.getId());
                    assertThat(usage.getProspectosCount()).isZero();
                    assertThat(usage.getOportunidadesCount()).isZero();
                    assertThat(usage.getLandingsCount()).isZero();
                });
    }
}
