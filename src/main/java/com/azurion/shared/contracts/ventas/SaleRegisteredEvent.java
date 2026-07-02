package com.azurion.shared.contracts.ventas;

import com.azurion.shared.event.DomainEvent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SaleRegisteredEvent(
        String tenantId,
        String saleId,
        String customerDocument,
        String customerName,
        String currency,
        BigDecimal total,
        List<SaleItem> items,
        OffsetDateTime occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "saascore.sale.registered";
    }

    public record SaleItem(
            String sku,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
