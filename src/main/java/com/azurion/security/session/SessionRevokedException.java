package com.azurion.security.session;

public class SessionRevokedException extends RuntimeException {

    public SessionRevokedException() {
        super("La sesión ya no está activa");
    }
}
