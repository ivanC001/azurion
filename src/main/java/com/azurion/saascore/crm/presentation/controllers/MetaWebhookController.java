package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.crm.application.dto.MetaWebhookResult;
import com.azurion.saascore.crm.application.services.MetaWebhookService;
import com.azurion.saascore.crm.application.services.PublicCrmTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/public/crm/meta", "/public/crm/meta"})
@RequiredArgsConstructor
public class MetaWebhookController {

    private final MetaWebhookService metaWebhookService;
    private final PublicCrmTenantResolver tenantResolver;

    @GetMapping(value = "/{tenantReference}/{channel}/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @PathVariable String tenantReference,
            @PathVariable String channel,
            @RequestParam(name = "hub.mode") String mode,
            @RequestParam(name = "hub.verify_token") String verifyToken,
            @RequestParam(name = "hub.challenge") String challenge) {
        tenantResolver.resolve(tenantReference);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(metaWebhookService.verify(channel, mode, verifyToken, challenge));
    }

    @PostMapping(value = "/{tenantReference}/{channel}/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receive(
            @PathVariable String tenantReference,
            @PathVariable String channel,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        tenantResolver.resolve(tenantReference);
        MetaWebhookResult result = metaWebhookService.receive(channel, rawBody, signature);
        return ResponseEntity.ok("EVENT_RECEIVED:" + result.eventosRecibidos());
    }

}
