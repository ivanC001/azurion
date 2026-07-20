package com.azurion.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.azurion.shared.api.ApiError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void exposesUserCorrectableBusinessValidation() {
        MDC.put("traceId", "trace-validation");

        ResponseEntity<ApiError> response = handler.handleBusiness(new BusinessException(
                "CRM_CLIENTE_DOCUMENTO_REQUERIDO",
                "El prospecto necesita numero de documento para convertirse en cliente"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CRM_CLIENTE_DOCUMENTO_REQUERIDO");
        assertThat(response.getBody().message())
                .isEqualTo("El prospecto necesita numero de documento para convertirse en cliente");
        assertThat(response.getBody().userActionable()).isTrue();
        assertThat(response.getBody().traceId()).isEqualTo("trace-validation");
    }

    @Test
    void mapsMissingBusinessResourceToNotFound() {
        ResponseEntity<ApiError> response = handler.handleBusiness(new BusinessException(
                "PRODUCTO_NO_ENCONTRADO",
                "Producto no encontrado"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Producto no encontrado");
    }

    @Test
    void hidesTechnicalTransportFailure() {
        MDC.put("traceId", "trace-email");

        ResponseEntity<ApiError> response = handler.handleBusiness(new BusinessException(
                "EMAIL_SEND_ERROR",
                "Connection refused by smtp.internal:587"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("OPERATION_FAILED");
        assertThat(response.getBody().message()).doesNotContain("smtp.internal", "Connection refused");
        assertThat(response.getBody().details()).isEmpty();
        assertThat(response.getBody().userActionable()).isFalse();
        assertThat(response.getBody().traceId()).isEqualTo("trace-email");
    }

    @Test
    void explicitInternalBusinessFailureIsNeverExposed() {
        ResponseEntity<ApiError> response = handler.handleBusiness(BusinessException.internal(
                "CUSTOM_PROVIDER_FAILURE",
                "secret provider response"
        ));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("OPERATION_FAILED");
        assertThat(response.getBody().message()).doesNotContain("secret provider response");
        assertThat(response.getBody().userActionable()).isFalse();
    }

    @Test
    void preservesExplicitConflictStatusForUserCorrectableFailures() {
        ResponseEntity<ApiError> response = handler.handleBusiness(BusinessException.conflict(
                "COTIZACION_EMAIL_ESTADO_INCIERTO",
                "El envio anterior debe revisarse"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("COTIZACION_EMAIL_ESTADO_INCIERTO");
        assertThat(response.getBody().userActionable()).isTrue();
    }

    @Test
    void hidesAuthenticationDetailButPreservesUnauthorizedStatus() {
        ResponseEntity<ApiError> response = handler.handleBusiness(BusinessException.unauthorized(
                "FACTURADOR_CALLBACK_SIGNATURE_INVALID",
                "firma interna esperada"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("firma interna esperada");
        assertThat(response.getBody().userActionable()).isFalse();
    }

    @Test
    void hidesFacturadorConfigurationDetails() {
        ResponseEntity<ApiError> response = handler.handleBusiness(new BusinessException(
                "FACTURADOR_API_KEY_MISSING",
                "No existe API key para el tenant privado 20123456789"
        ));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("OPERATION_FAILED");
        assertThat(response.getBody().message()).doesNotContain("API key", "20123456789");
        assertThat(response.getBody().userActionable()).isFalse();
    }

    @Test
    void hidesUnexpectedExceptionAndReturnsTrackingId() {
        MDC.put("traceId", "trace-unknown");

        ResponseEntity<ApiError> response = handler.handleUnknown(
                new IllegalStateException("jdbc password leaked by driver")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("jdbc", "password", "driver");
        assertThat(response.getBody().userActionable()).isFalse();
        assertThat(response.getBody().traceId()).isEqualTo("trace-unknown");
    }
}
