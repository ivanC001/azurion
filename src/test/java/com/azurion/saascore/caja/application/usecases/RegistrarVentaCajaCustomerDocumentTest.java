package com.azurion.saascore.caja.application.usecases;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistrarVentaCajaCustomerDocumentTest {

    @Test
    void acceptsPersonIdentifiedWithDni() {
        assertTrue(RegistrarVentaCajaUseCase.isIdentifiedCustomer("1", "12345678", "Cliente Persona"));
    }

    @Test
    void acceptsCompanyIdentifiedWithRuc() {
        assertTrue(RegistrarVentaCajaUseCase.isIdentifiedCustomer("6", "20123456789", "Empresa SAC"));
    }

    @Test
    void rejectsInvalidDocumentOrMissingName() {
        assertFalse(RegistrarVentaCajaUseCase.isIdentifiedCustomer("1", "123", "Cliente Persona"));
        assertFalse(RegistrarVentaCajaUseCase.isIdentifiedCustomer("6", "20123456789", " "));
    }
}
