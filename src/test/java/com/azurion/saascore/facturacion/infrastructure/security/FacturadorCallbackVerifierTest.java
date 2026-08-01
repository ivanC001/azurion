package com.azurion.saascore.facturacion.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.azurion.saascore.facturacion.domain.repositories.FacturadorCallbackNonceRepository;
import com.azurion.saascore.facturacion.infrastructure.config.FacturadorCallbackProperties;
import com.azurion.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class FacturadorCallbackVerifierTest {

    @Test
    void disabledCallbackReceiverRejectsInsteadOfSkippingAuthentication() {
        FacturadorCallbackProperties properties = new FacturadorCallbackProperties();
        properties.setEnabled(false);
        FacturadorCallbackVerifier verifier = new FacturadorCallbackVerifier(
                properties,
                mock(FacturadorCallbackNonceRepository.class)
        );

        assertThatThrownBy(() -> verifier.verify(new MockHttpServletRequest(), "{}"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getCode())
                            .isEqualTo("FACTURADOR_CALLBACK_DISABLED");
                    org.assertj.core.api.Assertions.assertThat(exception.getStatus())
                            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }
}
