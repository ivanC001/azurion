package com.azurion.shared.exception;

public class BusinessException extends RuntimeException {

    private final String code;
    private final boolean userActionable;

    public BusinessException(String code, String message) {
        this(code, message, true);
    }

    private BusinessException(String code, String message, boolean userActionable) {
        super(message);
        this.code = code;
        this.userActionable = userActionable;
    }

    public static BusinessException internal(String code, String message) {
        return new BusinessException(code, message, false);
    }

    public String getCode() {
        return code;
    }

    public boolean isUserActionable() {
        return userActionable;
    }
}
