package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.UpdateWhatsappAutoReplyConfigRequest;
import com.azurion.saascore.crm.application.dto.WhatsappAutoReplyConfigResponse;
import com.azurion.saascore.crm.application.dto.WhatsappAutoReplyScheduleRequest;
import com.azurion.saascore.crm.application.dto.WhatsappAutoReplyScheduleResponse;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplyConfig;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplySchedule;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappAutoReplyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappAutoReplyScheduleRepository;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsappAutoReplyConfigurationService {

    private static final String DEFAULT_ZONE = "America/Lima";
    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(18, 0);

    private final CrmWhatsappAutoReplyConfigRepository configRepository;
    private final CrmWhatsappAutoReplyScheduleRepository scheduleRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public WhatsappAutoReplyConfigResponse getConfiguration() {
        CrmWhatsappAutoReplyConfig config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null) {
            return new WhatsappAutoReplyConfigResponse(
                    false,
                    "SIEMPRE",
                    "",
                    720,
                    tenantZone().getId(),
                    defaultSchedules()
            );
        }
        return toResponse(config, scheduleRepository.findAllByConfig_IdOrderByDiaSemanaAsc(config.getId()));
    }

    @Transactional
    public WhatsappAutoReplyConfigResponse updateConfiguration(UpdateWhatsappAutoReplyConfigRequest request) {
        String mode = request.modo().trim().toUpperCase(Locale.ROOT);
        String message = request.mensaje() == null ? "" : request.mensaje().trim();
        if (Boolean.TRUE.equals(request.activo()) && message.isBlank()) {
            throw new BusinessException(
                    "CRM_WHATSAPP_AUTO_MENSAJE_REQUERIDO",
                    "Escribe el mensaje que WhatsApp enviara automaticamente"
            );
        }

        Map<Integer, WhatsappAutoReplyScheduleRequest> normalized = normalizeSchedules(request.horarios());
        if ("HORARIO".equals(mode) && Boolean.TRUE.equals(request.activo())
                && normalized.values().stream().noneMatch(item -> Boolean.TRUE.equals(item.activo()))) {
            throw new BusinessException(
                    "CRM_WHATSAPP_AUTO_HORARIO_REQUERIDO",
                    "Activa al menos un dia para usar la respuesta automatica por horario"
            );
        }

        CrmWhatsappAutoReplyConfig config = configRepository.findFirstByOrderByIdAsc()
                .orElseGet(CrmWhatsappAutoReplyConfig::new);
        config.setActivo(Boolean.TRUE.equals(request.activo()));
        config.setModo(mode);
        config.setMensaje(message);
        config.setCooldownMinutos(request.cooldownMinutos());
        CrmWhatsappAutoReplyConfig saved = configRepository.save(config);

        scheduleRepository.deleteAllByConfig_Id(saved.getId());
        scheduleRepository.flush();
        List<CrmWhatsappAutoReplySchedule> schedules = new ArrayList<>(7);
        for (int day = 1; day <= 7; day++) {
            WhatsappAutoReplyScheduleRequest source = normalized.get(day);
            CrmWhatsappAutoReplySchedule schedule = new CrmWhatsappAutoReplySchedule();
            schedule.setConfig(saved);
            schedule.setDiaSemana(day);
            schedule.setHoraInicio(source == null ? DEFAULT_START : source.horaInicio());
            schedule.setHoraFin(source == null ? DEFAULT_END : source.horaFin());
            schedule.setActivo(source != null && Boolean.TRUE.equals(source.activo()));
            schedules.add(schedule);
        }
        return toResponse(saved, scheduleRepository.saveAll(schedules));
    }

    public ZoneId tenantZone() {
        String configured = empresaRepository.findByTenantId(TenantContext.getTenantId())
                .map(empresa -> empresa.getZonaHoraria())
                .filter(value -> value != null && !value.isBlank())
                .orElse(DEFAULT_ZONE);
        try {
            return ZoneId.of(configured);
        } catch (RuntimeException ignored) {
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    private Map<Integer, WhatsappAutoReplyScheduleRequest> normalizeSchedules(
            List<WhatsappAutoReplyScheduleRequest> schedules
    ) {
        Map<Integer, WhatsappAutoReplyScheduleRequest> normalized = new HashMap<>();
        for (WhatsappAutoReplyScheduleRequest schedule : schedules) {
            if (normalized.put(schedule.diaSemana(), schedule) != null) {
                throw new BusinessException(
                        "CRM_WHATSAPP_AUTO_DIA_DUPLICADO",
                        "Cada dia solo puede configurarse una vez"
                );
            }
            if (schedule.horaInicio().equals(schedule.horaFin())) {
                throw new BusinessException(
                        "CRM_WHATSAPP_AUTO_RANGO_INVALIDO",
                        "La hora de inicio y fin deben ser diferentes"
                );
            }
        }
        return normalized;
    }

    private WhatsappAutoReplyConfigResponse toResponse(
            CrmWhatsappAutoReplyConfig config,
            List<CrmWhatsappAutoReplySchedule> schedules
    ) {
        List<WhatsappAutoReplyScheduleResponse> responseSchedules = schedules.isEmpty()
                ? defaultSchedules()
                : schedules.stream()
                        .map(item -> new WhatsappAutoReplyScheduleResponse(
                                item.getDiaSemana(),
                                item.getHoraInicio(),
                                item.getHoraFin(),
                                item.isActivo()
                        ))
                        .toList();
        return new WhatsappAutoReplyConfigResponse(
                config.isActivo(),
                config.getModo(),
                config.getMensaje() == null ? "" : config.getMensaje(),
                config.getCooldownMinutos(),
                tenantZone().getId(),
                responseSchedules
        );
    }

    private List<WhatsappAutoReplyScheduleResponse> defaultSchedules() {
        List<WhatsappAutoReplyScheduleResponse> schedules = new ArrayList<>(7);
        for (int day = 1; day <= 7; day++) {
            schedules.add(new WhatsappAutoReplyScheduleResponse(day, DEFAULT_START, DEFAULT_END, day <= 5));
        }
        return schedules;
    }
}
