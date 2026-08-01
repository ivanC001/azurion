package com.azurion.saascore.inventory.application.dto;

public record ProductSummaryResponse(
        long total,
        long active,
        long products,
        long services,
        long lowStock
) {
}
