package com.azurion.saascore.settings.email.application.services;

import com.azurion.saascore.settings.email.domain.entities.SmtpSecurity;
import com.azurion.saascore.settings.email.domain.entities.TenantEmailConfig;
import com.azurion.saascore.settings.email.infrastructure.config.EmailTransportProperties;
import com.azurion.shared.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailTransportService {

    private final EmailSecretEncryptionService encryptionService;
    private final EmailTransportProperties transportProperties;

    public void send(TenantEmailConfig config, String to, String subject, String body, List<EmailAttachment> attachments) {
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(config.getSmtpHost());
            sender.setPort(config.getSmtpPort());
            sender.setUsername(config.getSmtpUsername());
            sender.setPassword(encryptionService.decrypt(config.getSmtpPasswordEncrypted()));
            sender.setJavaMailProperties(mailProperties(config.getSmtpSecurity()));

            MimeMessage message = sender.createMimeMessage();
            boolean multipart = attachments != null && !attachments.isEmpty();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
            helper.setFrom(config.getCorreoRemitente(), config.getNombreRemitente());
            if (config.getReplyTo() != null && !config.getReplyTo().isBlank()) {
                helper.setReplyTo(config.getReplyTo());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            if (multipart) {
                for (EmailAttachment attachment : attachments) {
                    if (attachment == null || attachment.content() == null || attachment.content().length == 0) {
                        continue;
                    }
                    InputStreamSource source = () -> new ByteArrayInputStream(attachment.content());
                    helper.addAttachment(attachment.filename(), source);
                }
            }
            sender.send(message);
        } catch (Exception ex) {
            log.warn(
                    "Fallo SMTP tenant={} host={} port={} security={} errorType={} detail={}",
                    config.getTenantId(),
                    config.getSmtpHost(),
                    config.getSmtpPort(),
                    config.getSmtpSecurity(),
                    rootCause(ex).getClass().getSimpleName(),
                    sanitizeError(rootCause(ex))
            );
            throw new BusinessException("EMAIL_SEND_ERROR", sanitizeError(ex));
        }
    }

    private Properties mailProperties(SmtpSecurity security) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", Integer.toString(transportProperties.getConnectTimeoutMillis()));
        properties.put("mail.smtp.timeout", Integer.toString(transportProperties.getReadTimeoutMillis()));
        properties.put("mail.smtp.writetimeout", Integer.toString(transportProperties.getWriteTimeoutMillis()));
        properties.put("mail.smtp.ssl.checkserveridentity", Boolean.toString(transportProperties.isCheckServerIdentity()));
        if (transportProperties.getTlsProtocols() != null && !transportProperties.getTlsProtocols().isBlank()) {
            properties.put("mail.smtp.ssl.protocols", transportProperties.getTlsProtocols().trim());
        }
        if (security == SmtpSecurity.TLS) {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }
        if (security == SmtpSecurity.SSL) {
            properties.put("mail.smtp.ssl.enable", "true");
        }
        return properties;
    }

    private String sanitizeError(Throwable ex) {
        Throwable cause = rootCause(ex);
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)(password|pass|pwd|secret|token)=\\S+", "$1=***");
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
