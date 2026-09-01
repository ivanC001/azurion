package com.azurion.saascore.crm.domain;

import static org.junit.jupiter.api.Assertions.*;
import com.azurion.shared.exception.BusinessException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class WhatsappTemplateTest {
    private WhatsappTemplate template() {
        return new WhatsappTemplate("1", "seguimiento_prospecto", "es_PE", "APPROVED", "MARKETING",
                List.of(new WhatsappTemplate.Component("BODY", "Hola {{1}}, tu solicitud sobre {{2}}.", List.of("1", "2"))), null);
    }

    @Test void validatesAndRendersTheSelectedValuesWithoutChangingCurrencyCharacters() {
        var template = template();
        var values = template.validateParameters(List.of("  Ivan Flores ", "Curso $10 \\ Python"));
        assertEquals("Hola Ivan Flores, tu solicitud sobre Curso $10 \\ Python.", template.render(values));
    }

    @Test void identifiesTheMissingVariable() {
        var error = assertThrows(BusinessException.class,
                () -> template().validateParameters(List.of("Ivan", "   ")));
        assertEquals("CRM_WHATSAPP_PARAMETRO_VACIO", error.getCode());
        assertTrue(error.getMessage().contains("{{2}}"));
    }

    @Test void rejectsNullValuesWrongCountsAndLiteralPlaceholders() {
        for (var values : List.of(Arrays.asList("Ivan", null), List.of("Ivan"), List.of("Ivan", "{{2}}"),
                List.of("Ivan", "Curso\nPython"), List.of("Ivan", "x".repeat(1025)))) {
            assertThrows(BusinessException.class, () -> template().validateParameters(values));
        }
    }

    @Test void supportsNamedVariablesAndIndependentHeaderIndexes() {
        var template = new WhatsappTemplate("2", "other_name", "pt_BR", "APPROVED", "UTILITY", List.of(
                new WhatsappTemplate.Component("HEADER", "Solicitud {{1}}", List.of("1")),
                new WhatsappTemplate.Component("BODY", "Ola {{nome}}, {{curso}} / {{curso}}", List.of("nome", "curso")),
                new WhatsappTemplate.Component("FOOTER", "Gracias", List.of())), null);
        assertEquals(3, template.parameterCount());
        assertEquals("Solicitud 123\n\nOla Ana, Java / Java\n\nGracias", template.render(List.of("123", "Ana", "Java")));
    }

    @Test void ordersNumericParametersRatherThanOccurrenceAndDeduplicatesRepeatedTokens() {
        assertEquals(List.of("1", "2", "10"), WhatsappTemplate.parameterNames("{{10}} {{2}} {{1}} {{2}}"));
    }

    @Test void refusesUnavailableTemplates() {
        var template = new WhatsappTemplate("1", "paused", "es_PE", "PAUSED", "UTILITY", List.of(), null);
        assertThrows(BusinessException.class, () -> template.validateParameters(List.of()));
    }
}
