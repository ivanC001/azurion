package com.azurion.saascore.settings.email.application.services;

import com.azurion.saascore.settings.email.domain.entities.PlatformEmailConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformEmailDeliveryService {

    private final PlatformEmailConfigService configService;
    private final SmtpEmailTransportService smtpTransport;

    public void sendNotification(String to, String subject, String body) {
        send(PlatformEmailPurpose.NOTIFICATION, to, subject, body, List.of());
    }

    public void sendReport(String to, String subject, String body, List<EmailAttachment> attachments) {
        send(PlatformEmailPurpose.REPORT, to, subject, body, attachments);
    }

    public void sendTwoFactorCode(String to, String code) {
        String body = "Tu codigo de verificacion de Azurion es: " + code
                + ". Si no solicitaste este acceso, ignora el mensaje y reportalo al administrador.";
        send(PlatformEmailPurpose.TWO_FACTOR, to, "Codigo de verificacion de Azurion", body, List.of());
    }

    private void send(
            PlatformEmailPurpose purpose,
            String to,
            String subject,
            String body,
            List<EmailAttachment> attachments
    ) {
        PlatformEmailConfig config = configService.getVerifiedConfigOrThrow(purpose);
        smtpTransport.send(config, to, subject, body, attachments == null ? List.of() : attachments);
    }
}
