package com.azurion.saascore.facturacion.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "facturador_callback_nonces", schema = "public")
public class FacturadorCallbackNonce extends BaseEntity {

    @Column(name = "nonce_key_hash", nullable = false, unique = true, length = 64)
    private String nonceKeyHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public String getNonceKeyHash() {
        return nonceKeyHash;
    }

    public void setNonceKeyHash(String nonceKeyHash) {
        this.nonceKeyHash = nonceKeyHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
