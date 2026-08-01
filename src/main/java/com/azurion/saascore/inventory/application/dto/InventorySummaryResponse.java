package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;

public record InventorySummaryResponse(
        long stockLines,
        long lowStock,
        long noStock,
        long expiring,
        long expired,
        long movements,
        long purchases,
        BigDecimal invested,
        BigDecimal projectedProfit
) {
}
