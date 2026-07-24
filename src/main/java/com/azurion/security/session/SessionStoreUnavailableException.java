package com.azurion.security.session;

public class SessionStoreUnavailableException extends RuntimeException {

    public SessionStoreUnavailableException(Throwable cause) {
        super("El servicio de sesiones no esta disponible", cause);
    }
}
