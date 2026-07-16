package com.azurion.saascore.settings.email.application.services;

import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private final TenantEmailConfigService configService;
    private final SmtpEmailTransportService smtpTransport;

    public void sendEmail(String tenantId, String to, String subject, String body, List<EmailAttachment> attachments) {
        TenantEmailConfig config = configService.getVerifiedConfigOrThrow(tenantId);
        smtpTransport.send(config, to, subject, body, attachments == null ? List.of() : attachments);
    }
}
