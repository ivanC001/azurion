package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

public record SendCrmOpportunityEmailResponse(
        String destinatario,
        String asunto,
        OffsetDateTime enviadoEn
) {
}
