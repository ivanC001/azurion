package com.azurion.saascore.messaging.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "platform_message_recipients", schema = "public")
public class PlatformMessageRecipient extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private PlatformMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_scope", nullable = false, length = 20)
    private MessageRecipientScope recipientScope;

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username_snapshot", nullable = false, length = 120)
    private String usernameSnapshot;

    @Column(name = "display_name_snapshot", length = 180)
    private String displayNameSnapshot;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
