package com.azurion.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final boolean userActionable;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, true);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, true);
    }

    private BusinessException(String code, String message, HttpStatus status, boolean userActionable) {
        super(message);
        this.code = code;
        this.userActionable = userActionable;
        this.status = status == null ? HttpStatus.BAD_REQUEST : status;
    }

    public static BusinessException internal(String code, String message) {
        return new BusinessException(code, message, HttpStatus.INTERNAL_SERVER_ERROR, false);
    }

    public static BusinessException unauthorized(String code, String message) {
        return new BusinessException(code, message, HttpStatus.UNAUTHORIZED, false);
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(code, message, HttpStatus.FORBIDDEN, false);
    }

    public static BusinessException notFound(String code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND, true);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT, true);
    }

    public String getCode() {
        return code;
    }

    public boolean isUserActionable() {
        return userActionable;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
