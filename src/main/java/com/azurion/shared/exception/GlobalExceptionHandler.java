package com.azurion.shared.exception;

import com.azurion.shared.api.ApiError;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_OPERATION_ERROR =
            "No se pudo completar la operacion en este momento. Intenta nuevamente.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        if (!ErrorExposurePolicy.isUserActionable(ex)) {
            log.error("Internal business failure code={} traceId={}", ex.getCode(), traceId(), ex);
            return response(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OPERATION_FAILED",
                    GENERIC_OPERATION_ERROR,
                    List.of(),
                    false
            );
        }

        return response(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), List.of(), true);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Revisa los datos ingresados",
                details,
                true
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBinding(BindException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Revisa los datos ingresados",
                details,
                true
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> details = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> {
                            String parameter = result.getMethodParameter().getParameterName();
                            String message = error.getDefaultMessage();
                            return (parameter == null ? "parametro" : parameter) + ": " + message;
                        }))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Revisa los datos ingresados",
                details,
                true
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Revisa los datos ingresados",
                details,
                true
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex) {
        return response(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "No tienes permiso para realizar esta operacion",
                List.of(),
                true
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "AUTH_ERROR",
                "Usuario o contrasena incorrectos",
                List.of(),
                true
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidJson(HttpMessageNotReadableException ex) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_JSON",
                "Los datos enviados no tienen un formato valido",
                List.of(),
                true
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "El parametro '" + ex.getName() + "' tiene un formato invalido",
                List.of(),
                true
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        String message = "Los datos ingresados entran en conflicto con un registro existente";
        if (cause != null && cause.contains("productos_sku_key")) {
            message = "Ya existe un producto con ese SKU";
        } else if (cause != null && cause.contains("uk_productos_codigo_not_null")) {
            message = "Ya existe un producto con ese codigo";
        } else if (cause != null && cause.contains("value too long")) {
            message = "Uno de los datos ingresados supera el tamano permitido";
        }

        log.warn("Data integrity violation traceId={} cause={}", traceId(), cause);
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", message, List.of(), true);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MISSING_HEADER",
                "Falta el dato requerido: " + ex.getHeaderName(),
                List.of(),
                true
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Falta el parametro requerido: " + ex.getParameterName(),
                List.of(),
                true
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUEST_PART",
                "Falta el archivo o dato requerido: " + ex.getRequestPartName(),
                List.of(),
                true
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return response(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                "No se encontro el recurso solicitado",
                List.of(),
                true
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "La operacion solicitada no esta disponible",
                List.of(),
                true
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "El archivo supera el limite permitido de 8 MB",
                List.of(),
                true
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipart(MultipartException ex) {
        log.error("Multipart processing failure traceId={}", traceId(), ex);
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_MULTIPART_REQUEST",
                "No se pudo procesar el archivo adjunto. Verifica el archivo e intenta nuevamente.",
                List.of(),
                false
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex) {
        log.error("Unhandled exception traceId={}", traceId(), ex);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                GENERIC_OPERATION_ERROR,
                List.of(),
                false
        );
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String code,
            String message,
            List<String> details,
            boolean userActionable
    ) {
        return ResponseEntity.status(status).body(new ApiError(
                code,
                message,
                details,
                OffsetDateTime.now(),
                userActionable,
                traceId()
        ));
    }

    private String traceId() {
        String currentTraceId = MDC.get("traceId");
        return currentTraceId == null || currentTraceId.isBlank()
                ? UUID.randomUUID().toString()
                : currentTraceId;
    }
}
