package com.azurion.security.session;

public class ActiveSessionExistsException extends RuntimeException {

    private final AuthSessionRecord activeSession;
    private final String replacementToken;

    public ActiveSessionExistsException(
            AuthSessionRecord activeSession,
            String replacementToken
    ) {
        super("Esta cuenta ya tiene una sesión activa en otro dispositivo");
        this.activeSession = activeSession;
        this.replacementToken = replacementToken;
    }

    public AuthSessionRecord getActiveSession() {
        return activeSession;
    }

    public String getReplacementToken() {
        return replacementToken;
    }
}
