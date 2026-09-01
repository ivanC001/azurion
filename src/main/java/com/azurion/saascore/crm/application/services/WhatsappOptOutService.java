package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de WhatsApp por prospecto.
 *
 * <p>Depende solo de repositorios a proposito: {@code WhatsappIntegrationService} lo
 * usa al procesar mensajes entrantes, y si dependiera del servicio de reenganches
 * (que a su vez necesita al de integracion para validar plantillas) el contexto de
 * Spring quedaria con un ciclo.
 *
 * <p>Honrar la baja no es opcional: es politica de Meta, y seguir escribiendo a quien
 * pidio parar degrada la calidad del numero hasta que Meta bloquea los envios.
 */
@Service
@RequiredArgsConstructor
public class WhatsappOptOutService {

    /**
     * Frases de baja. Se comparan contra el mensaje completo normalizado, no como
     * subcadena: "no quiero cancelar la compra" no es una baja.
     */
    private static final Set<String> FRASES_DE_BAJA = Set.of(
            "stop",
            "baja",
            "darme de baja",
            "dar de baja",
            "no molestar",
            "no escribir",
            "no escribir mas",
            "no me escribas",
            "no me escriban",
            "no quiero mas mensajes",
            "no deseo mas mensajes",
            "desuscribir",
            "desuscribirme",
            "unsubscribe",
            "cancelar suscripcion",
            "eliminar mis datos"
    );

    private final CrmProspectoRepository prospectoRepository;
    private final CrmWhatsappReengagementOutboxRepository outboxRepository;

    /**
     * Marca la baja si el texto entrante la pide. Devuelve {@code true} si la aplico.
     */
    @Transactional
    public boolean applyIfRequested(CrmProspecto prospecto, String cuerpo) {
        if (prospecto == null || !esPedidoDeBaja(cuerpo)) {
            return false;
        }
        optOut(prospecto.getId(), "El cliente respondio: " + cuerpo.trim());
        return true;
    }

    @Transactional
    public void optOut(Long prospectoId, String motivo) {
        prospectoRepository.findById(prospectoId).ifPresent(prospecto -> {
            if (prospecto.getWhatsappOptoutEn() == null) {
                prospecto.setWhatsappOptoutEn(OffsetDateTime.now(ZoneOffset.UTC));
                prospecto.setWhatsappOptoutMotivo(truncate(motivo, 200));
                prospectoRepository.save(prospecto);
            }
        });
        outboxRepository.cancelPendingForProspecto(
                TenantContext.getTenantId(),
                prospectoId,
                "El cliente pidio no recibir mas mensajes",
                LocalDateTime.now()
        );
    }

    @Transactional
    public void optIn(Long prospectoId) {
        prospectoRepository.findById(prospectoId).ifPresent(prospecto -> {
            prospecto.setWhatsappOptoutEn(null);
            prospecto.setWhatsappOptoutMotivo(null);
            prospectoRepository.save(prospecto);
        });
    }

    boolean esPedidoDeBaja(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return false;
        }
        String normalizado = normalizar(cuerpo);
        // Un mensaje largo que menciona "baja" casi nunca es una baja.
        return normalizado.length() <= 40 && FRASES_DE_BAJA.contains(normalizado);
    }

    private String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Se descarta todo lo que no sea letra, digito o espacio: asi "¡BAJA!" y "stop."
        // llegan igual que "baja" y "stop". \p{Punct} solo cubre ASCII y dejaria fuera
        // los signos de apertura del castellano.
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
