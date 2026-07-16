package com.azurion.saascore.settings.email.application.services;

public record EmailAttachment(
        String filename,
        String contentType,
        byte[] content
) {
}
