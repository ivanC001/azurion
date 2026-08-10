package com.azurion.saascore.facturacion.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.saascore.facturacion.application.dto.FacturadorTenantConfigurationRequest;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FacturadorTenantPayloadFactoryTest {

    private final FacturadorTenantPayloadFactory factory =
            new FacturadorTenantPayloadFactory(new ObjectMapper());

    @Test
    void normalizesConfiguredBankAccounts() {
        FacturadorTenantConfigurationRequest request = new FacturadorTenantConfigurationRequest();
        request.setCuentas_bancarias_json("""
                [{"banco":"Banco Demo","moneda":"pen","cuenta":"001-234","cci":"00200123456789012345"}]
                """);

        Map<String, Object> payload = factory.create(request);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> accounts = (List<Map<String, String>>) payload.get("cuentas_bancarias");
        assertThat(accounts).containsExactly(Map.of(
                "banco", "Banco Demo",
                "moneda", "PEN",
                "cuenta", "001-234",
                "cci", "00200123456789012345"
        ));
    }

    @Test
    void rejectsIncompleteBankAccounts() {
        FacturadorTenantConfigurationRequest request = new FacturadorTenantConfigurationRequest();
        request.setCuentas_bancarias_json("[{\"banco\":\"Banco Demo\",\"moneda\":\"PEN\"}]");

        assertThatThrownBy(() -> factory.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("numero de cuenta");
    }
}
