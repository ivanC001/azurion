package com.azurion.saascore.crm.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azurion.saascore.crm.domain.WhatsappTemplateDraft.Button;
import com.azurion.saascore.crm.domain.WhatsappTemplateDraft.Component;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class WhatsappTemplateDraftTest {

    private WhatsappTemplateDraft draft(
            String nombre, String categoria, Component header, Component body,
            String footer, List<Button> buttons) {
        return new WhatsappTemplateDraft(nombre, "es", categoria, header, body, footer, buttons);
    }

    private Component cuerpoValido() {
        return new Component(
                "Hola {{1}}, tu cotizacion #{{2}} vence el {{3}}.",
                List.of("Carlos", "8421", "15/09/2026")
        );
    }

    @Test
    void normalizaElBorradorCompleto() {
        WhatsappTemplateDraft validado = draft(
                "  Seguir_Cotizacion  ", "utility",
                new Component("Cotizacion {{1}}", List.of("8421")),
                cuerpoValido(),
                "  Azurion CRM  ",
                List.of(new Button("quick_reply", " Acepto ", null, null))
        ).validated();

        assertEquals("seguir_cotizacion", validado.name());
        assertEquals("UTILITY", validado.category());
        assertEquals("Azurion CRM", validado.footer());
        assertEquals("QUICK_REPLY", validado.buttons().getFirst().type());
        assertEquals("Acepto", validado.buttons().getFirst().text());
    }

    @Test
    void rechazaNombresQueMetaNoAdmite() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("Seguir Cotizacion", "UTILITY", null, cuerpoValido(), null, List.of()).validated());

        assertEquals("CRM_WHATSAPP_PLANTILLA_BORRADOR_INVALIDO", error.getCode());
        assertTrue(error.getMessage().contains("minusculas"));
    }

    @Test
    void exigeUnCuerpo() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null, new Component("  ", List.of()), null, List.of()).validated());

        assertTrue(error.getMessage().contains("cuerpo"));
    }

    @Test
    void soloAdmiteUtilityYMarketing() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "AUTHENTICATION", null, cuerpoValido(), null, List.of()).validated());

        assertTrue(error.getMessage().contains("UTILITY"));
    }

    @Test
    void exigeQueLasVariablesVayanNumeradasEnOrden() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null,
                        new Component("Hola {{1}} sobre {{3}}", List.of("Carlos", "Curso")),
                        null, List.of()).validated());

        assertTrue(error.getMessage().contains("orden"));
    }

    @Test
    void detectaVariablesMalEscritas() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null,
                        new Component("Hola {{ 1 }}", List.of()), null, List.of()).validated());

        assertTrue(error.getMessage().contains("mal escrita"));
    }

    @Test
    void exigeUnEjemploPorVariable() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null,
                        new Component("Hola {{1}} y {{2}}", List.of("Carlos")),
                        null, List.of()).validated());

        assertTrue(error.getMessage().contains("ejemplo"));
    }

    @Test
    void dejaLosEjemplosEnUnaSolaLinea() {
        WhatsappTemplateDraft validado = draft("seguir", "UTILITY", null,
                new Component("Hola {{1}}", List.of("Carlos\n  Flores")), null, List.of()).validated();

        assertEquals(List.of("Carlos Flores"), validado.body().examples());
    }

    @Test
    void noAdmiteVariablesEnElPie() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null, cuerpoValido(), "Enviado por {{1}}", List.of()).validated());

        assertTrue(error.getMessage().contains("pie"));
    }

    @Test
    void descartaElEncabezadoVacio() {
        WhatsappTemplateDraft validado = draft("seguir", "UTILITY",
                new Component("   ", List.of()), cuerpoValido(), null, List.of()).validated();

        assertNull(validado.header());
    }

    @Test
    void rechazaBotonesDeEnlaceConVariables() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null, cuerpoValido(), null,
                        List.of(new Button("URL", "Ver", "https://azurion.tech/{{1}}", null))).validated());

        assertTrue(error.getMessage().contains("variables"));
    }

    @Test
    void rechazaBotonesRepetidosOSinDatos() {
        assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null, cuerpoValido(), null, List.of(
                        new Button("QUICK_REPLY", "Si", null, null),
                        new Button("QUICK_REPLY", "si", null, null))).validated());

        assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null, cuerpoValido(), null,
                        List.of(new Button("URL", "Ver", null, null))).validated());

        assertThrows(BusinessException.class, () ->
                draft("seguir", "UTILITY", null, cuerpoValido(), null,
                        List.of(new Button("PHONE_NUMBER", "Llamar", null, null))).validated());
    }

    /**
     * La razon de ser de esta clase: lo que se aprueba tiene que poder enviarse.
     */
    @Test
    void loQueValidaElBorradorLoPuedeEnviarElCompositor() {
        WhatsappTemplateDraft validado = draft(
                "seguir_cotizacion", "UTILITY",
                new Component("Cotizacion {{1}}", List.of("8421")),
                cuerpoValido(),
                "Azurion CRM",
                List.of(new Button("QUICK_REPLY", "Acepto", null, null))
        ).validated();

        WhatsappTemplate equivalente = new WhatsappTemplate(
                "t-1", validado.name(), validado.languageCode(), "APPROVED", validado.category(),
                List.of(
                        new WhatsappTemplate.Component("HEADER", validado.header().text(),
                                validado.header().parameters()),
                        new WhatsappTemplate.Component("BODY", validado.body().text(),
                                validado.body().parameters()),
                        new WhatsappTemplate.Component("FOOTER", validado.footer(), List.of()),
                        new WhatsappTemplate.Component("BUTTONS", "Acepto", List.of())
                ),
                null
        );

        assertTrue(equivalente.available());
        assertEquals(4, equivalente.parameterCount());
        assertEquals(
                List.of("8421", "Carlos", "8421", "15/09/2026"),
                equivalente.validateParameters(List.of("8421", "Carlos", "8421", "15/09/2026"))
        );
    }
}
