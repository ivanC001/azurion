package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

public record PublicLeadReceiptResponse(
        String receiptId,
        String status,
        OffsetDateTime receivedAt
) {
}
