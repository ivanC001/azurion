package com.azurion.saascore.settings.email.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "platform_email_config", schema = "public")
public class PlatformEmailConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false, unique = true, length = 80)
    private String configKey;

    @Column(name = "nombre_remitente", nullable = false, length = 160)
    private String nombreRemitente;

    @Column(name = "correo_remitente", nullable = false, length = 180)
    private String correoRemitente;

    @Column(name = "reply_to", length = 180)
    private String replyTo;

    @Column(name = "smtp_host", nullable = false, length = 180)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Enumerated(EnumType.STRING)
    @Column(name = "smtp_security", nullable = false, length = 10)
    private SmtpSecurity smtpSecurity = SmtpSecurity.TLS;

    @Column(name = "smtp_username", nullable = false, length = 180)
    private String smtpUsername;

    @Column(name = "smtp_password_encrypted", nullable = false, length = 3000)
    private String smtpPasswordEncrypted;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "verificado", nullable = false)
    private boolean verificado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private TenantEmailConfigStatus estado = TenantEmailConfigStatus.PENDIENTE;

    @Column(name = "avisos_habilitados", nullable = false)
    private boolean avisosHabilitados = true;

    @Column(name = "reportes_habilitados", nullable = false)
    private boolean reportesHabilitados = true;

    @Column(name = "doble_factor_habilitado", nullable = false)
    private boolean dobleFactorHabilitado = true;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;
}
