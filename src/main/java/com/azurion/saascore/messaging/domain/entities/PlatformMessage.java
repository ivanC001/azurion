package com.azurion.saascore.messaging.domain.entities;

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
@Table(name = "platform_messages", schema = "public")
public class PlatformMessage extends BaseEntity {

    @Column(name = "asunto", nullable = false, length = 180)
    private String asunto;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    private MessagePriority prioridad = MessagePriority.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "audiencia", nullable = false, length = 30)
    private MessageAudience audiencia;

    @Column(name = "tenant_id", length = 80)
    private String tenantId;

    @Column(name = "enviado_por_usuario_id", nullable = false)
    private Long enviadoPorUsuarioId;

    @Column(name = "enviado_por_username", nullable = false, length = 120)
    private String enviadoPorUsername;

    @Column(name = "publicado_en", nullable = false)
    private LocalDateTime publicadoEn;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public MessagePriority getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(MessagePriority prioridad) {
        this.prioridad = prioridad;
    }

    public MessageAudience getAudiencia() {
        return audiencia;
    }

    public void setAudiencia(MessageAudience audiencia) {
        this.audiencia = audiencia;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getEnviadoPorUsuarioId() {
        return enviadoPorUsuarioId;
    }

    public void setEnviadoPorUsuarioId(Long enviadoPorUsuarioId) {
        this.enviadoPorUsuarioId = enviadoPorUsuarioId;
    }

    public String getEnviadoPorUsername() {
        return enviadoPorUsername;
    }

    public void setEnviadoPorUsername(String enviadoPorUsername) {
        this.enviadoPorUsername = enviadoPorUsername;
    }

    public LocalDateTime getPublicadoEn() {
        return publicadoEn;
    }

    public void setPublicadoEn(LocalDateTime publicadoEn) {
        this.publicadoEn = publicadoEn;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(LocalDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
