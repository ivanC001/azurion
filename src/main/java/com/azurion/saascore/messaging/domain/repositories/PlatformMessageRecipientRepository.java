package com.azurion.saascore.messaging.domain.repositories;

import com.azurion.saascore.messaging.domain.entities.MessageRecipientScope;
import com.azurion.saascore.messaging.domain.entities.PlatformMessageRecipient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformMessageRecipientRepository extends JpaRepository<PlatformMessageRecipient, Long> {

    @EntityGraph(attributePaths = "message")
    @Query("""
            select recipient
              from PlatformMessageRecipient recipient
             where recipient.recipientScope = :scope
               and recipient.tenantId = :tenantId
               and recipient.userId = :userId
               and recipient.message.activo = true
               and recipient.message.publicadoEn <= :now
               and (recipient.message.expiraEn is null or recipient.message.expiraEn > :now)
             order by recipient.message.publicadoEn desc, recipient.id desc
            """)
    List<PlatformMessageRecipient> findInbox(
            @Param("scope") MessageRecipientScope scope,
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            select count(recipient)
              from PlatformMessageRecipient recipient
             where recipient.recipientScope = :scope
               and recipient.tenantId = :tenantId
               and recipient.userId = :userId
               and recipient.readAt is null
               and recipient.message.activo = true
               and recipient.message.publicadoEn <= :now
               and (recipient.message.expiraEn is null or recipient.message.expiraEn > :now)
            """)
    long countUnread(
            @Param("scope") MessageRecipientScope scope,
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    Optional<PlatformMessageRecipient> findByIdAndRecipientScopeAndTenantIdAndUserId(
            Long id,
            MessageRecipientScope scope,
            String tenantId,
            Long userId
    );

    @Modifying
    @Query("""
            update PlatformMessageRecipient recipient
               set recipient.readAt = :now
             where recipient.recipientScope = :scope
               and recipient.tenantId = :tenantId
               and recipient.userId = :userId
               and recipient.readAt is null
            """)
    int markAllRead(
            @Param("scope") MessageRecipientScope scope,
            @Param("tenantId") String tenantId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    long countByMessageId(Long messageId);

    long countByMessageIdAndReadAtIsNotNull(Long messageId);
}
