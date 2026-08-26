package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.crm.application.dto.CrmMetaResponse;
import com.azurion.saascore.crm.application.dto.SaveCrmMetaRequest;
import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import com.azurion.saascore.crm.domain.entities.CrmMeta;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmMetaRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrmGoalService {

    private static final String TEAM = "EQUIPO";
    private static final String ADVISOR = "ASESOR";

    private final CrmMetaRepository goalRepository;
    private final CrmOportunidadRepository opportunityRepository;
    private final CrmProspectoRepository prospectRepository;
    private final CrmActividadRepository activityRepository;
    private final CrmCurrencyConfigRepository currencyRepository;
    private final EmpresaRepository companyRepository;
    private final UsuarioTenantRepository userRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<CrmMetaResponse> list(Integer year, Integer month) {
        validatePeriod(year, month);
        PeriodPerformance performance = performance(year, month);
        String currentUser = currentUserKey();
        boolean viewTeam = canViewTeam();

        return goalRepository.findByAnioAndMesOrderByAlcanceAscResponsableIdAsc(year, month).stream()
                .filter(goal -> viewTeam
                        || TEAM.equals(goal.getAlcance())
                        || currentUser.equals(goal.getResponsableId()))
                .map(goal -> toResponse(goal, performance))
                .toList();
    }

    @Transactional
    public CrmMetaResponse save(SaveCrmMetaRequest request) {
        validatePeriod(request.anio(), request.mes());
        String scope = normalizeScope(request.alcance());
        String owner = ADVISOR.equals(scope) ? resolveAdvisor(request.responsableId()) : null;

        CrmMeta goal = TEAM.equals(scope)
                ? goalRepository.findByAnioAndMesAndAlcanceAndResponsableIdIsNull(
                        request.anio(), request.mes(), scope).orElseGet(CrmMeta::new)
                : goalRepository.findByAnioAndMesAndAlcanceAndResponsableId(
                        request.anio(), request.mes(), scope, owner).orElseGet(CrmMeta::new);

        goal.setAnio(request.anio());
        goal.setMes(request.mes());
        goal.setAlcance(scope);
        goal.setResponsableId(owner);
        goal.setMoneda(baseCurrency());
        goal.setMetaIngresos(money(request.metaIngresos()));
        goal.setMetaOportunidadesGanadas(request.metaOportunidadesGanadas());
        goal.setMetaProspectosNuevos(request.metaProspectosNuevos());
        goal.setMetaActividadesRealizadas(request.metaActividadesRealizadas());
        goal.setMetaConversion(percent(request.metaConversion()));
        if (goal.getId() == null) {
            goal.setCreatedBy(currentUserKey());
        }

        CrmMeta saved = goalRepository.save(goal);
        return toResponse(saved, performance(request.anio(), request.mes()));
    }

    @Transactional
    public void delete(Long id) {
        CrmMeta goal = goalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CRM_META_NO_ENCONTRADA", "La meta CRM no existe"));
        goalRepository.delete(goal);
    }

    private CrmMetaResponse toResponse(CrmMeta goal, PeriodPerformance performance) {
        OwnerPerformance actual = TEAM.equals(goal.getAlcance())
                ? performance.team()
                : performance.byOwner().getOrDefault(goal.getResponsableId(), OwnerPerformance.empty());
        BigDecimal actualConversion = actual.closed() == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(actual.won())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(actual.closed()), 2, RoundingMode.HALF_UP);

        return new CrmMetaResponse(
                goal.getId(),
                goal.getAnio(),
                goal.getMes(),
                goal.getAlcance(),
                goal.getResponsableId(),
                ownerName(goal.getResponsableId()),
                goal.getMoneda(),
                money(goal.getMetaIngresos()),
                goal.getMetaOportunidadesGanadas(),
                goal.getMetaProspectosNuevos(),
                goal.getMetaActividadesRealizadas(),
                percent(goal.getMetaConversion()),
                money(actual.revenue()),
                actual.won(),
                actual.prospects(),
                actual.activities(),
                actualConversion,
                progress(actual.revenue(), goal.getMetaIngresos()),
                progress(actual.won(), goal.getMetaOportunidadesGanadas()),
                progress(actual.prospects(), goal.getMetaProspectosNuevos()),
                progress(actual.activities(), goal.getMetaActividadesRealizadas()),
                progress(actualConversion, goal.getMetaConversion()),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    private PeriodPerformance performance(Integer year, Integer month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime from = firstDay.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime until = firstDay.plusMonths(1).atStartOfDay(zone).toOffsetDateTime();

        Map<String, MutableOwnerPerformance> owners = new LinkedHashMap<>();
        for (CrmOportunidadRepository.GoalAggregateProjection row
                : opportunityRepository.summarizeGoalsByOwner(from, until)) {
            MutableOwnerPerformance owner = owners.computeIfAbsent(
                    row.getResponsableId(), ignored -> new MutableOwnerPerformance());
            owner.won += row.getGanadas();
            owner.closed += row.getCerradas();
            owner.revenue = owner.revenue.add(toBaseCurrency(row.getMontoGanado(), row.getMoneda()));
        }

        LocalDateTime localFrom = firstDay.atStartOfDay();
        LocalDateTime localUntil = firstDay.plusMonths(1).atStartOfDay();
        for (CrmProspectoRepository.OwnerCountProjection row
                : prospectRepository.countGoalsByOwner(localFrom, localUntil)) {
            owners.computeIfAbsent(row.getResponsableId(), ignored -> new MutableOwnerPerformance())
                    .prospects += row.getCantidad();
        }
        for (CrmActividadRepository.OwnerCountProjection row
                : activityRepository.countGoalsByOwner(from, until)) {
            owners.computeIfAbsent(row.getResponsableId(), ignored -> new MutableOwnerPerformance())
                    .activities += row.getCantidad();
        }

        Map<String, OwnerPerformance> immutable = new HashMap<>();
        MutableOwnerPerformance team = new MutableOwnerPerformance();
        owners.forEach((ownerId, values) -> {
            OwnerPerformance value = values.toImmutable();
            immutable.put(ownerId, value);
            team.add(value);
        });
        return new PeriodPerformance(Map.copyOf(immutable), team.toImmutable());
    }

    private BigDecimal toBaseCurrency(BigDecimal amount, String sourceCurrency) {
        BigDecimal value = money(amount);
        String base = baseCurrency();
        if (sourceCurrency == null || base.equalsIgnoreCase(sourceCurrency)) {
            return value;
        }
        CrmCurrencyConfig config = currencyRepository.findByMoneda(sourceCurrency.toUpperCase(Locale.ROOT))
                .filter(CrmCurrencyConfig::isActivo)
                .orElse(null);
        if (config == null || config.getTipoCambioBase() == null
                || config.getTipoCambioBase().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal margin = config.getMargenConversionPorcentaje() == null
                ? BigDecimal.ZERO
                : config.getMargenConversionPorcentaje();
        BigDecimal rate = config.getTipoCambioBase().multiply(
                BigDecimal.ONE.add(margin.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
        return value.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private String baseCurrency() {
        return companyRepository.findByTenantId(TenantContext.getTenantId())
                .map(company -> company.getMonedaCodigo())
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .orElse("PEN");
    }

    private String resolveAdvisor(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new BusinessException("CRM_META_ASESOR_REQUERIDO", "Selecciona un asesor para la meta");
        }
        String value = requested.trim();
        UsuarioTenant user = null;
        try {
            user = userRepository.findById(Long.parseLong(value)).orElse(null);
        } catch (NumberFormatException ignored) {
            user = userRepository.findByUsernameAndActivoTrue(value).orElse(null);
        }
        if (user == null || !user.isActivo()) {
            throw new BusinessException("CRM_META_ASESOR_INVALIDO", "El asesor seleccionado no está activo");
        }
        return String.valueOf(user.getId());
    }

    private String ownerName(String ownerId) {
        if (ownerId == null) {
            return "Equipo comercial";
        }
        try {
            return userRepository.findById(Long.parseLong(ownerId))
                    .map(this::fullName)
                    .orElse(ownerId);
        } catch (NumberFormatException ignored) {
            return userRepository.findByUsernameAndActivoTrue(ownerId)
                    .map(this::fullName)
                    .orElse(ownerId);
        }
    }

    private String fullName(UsuarioTenant user) {
        String name = ((user.getNombres() == null ? "" : user.getNombres().trim()) + " "
                + (user.getApellidos() == null ? "" : user.getApellidos().trim())).trim();
        return name.isBlank() ? user.getUsername() : name;
    }

    private String normalizeScope(String value) {
        String scope = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!TEAM.equals(scope) && !ADVISOR.equals(scope)) {
            throw new BusinessException("CRM_META_ALCANCE_INVALIDO", "El alcance debe ser EQUIPO o ASESOR");
        }
        return scope;
    }

    private void validatePeriod(Integer year, Integer month) {
        if (year == null || year < 2020 || year > 2100 || month == null || month < 1 || month > 12) {
            throw new BusinessException("CRM_META_PERIODO_INVALIDO", "Selecciona un mes y año válidos");
        }
    }

    private boolean canViewTeam() {
        return hasAuthority("CRM_GOALS_MANAGE")
                || hasAuthority("CRM_REPORTS_TEAM")
                || hasAuthority("CRM_VIEW_ALL")
                || hasAuthority("ROLE_ADMIN_GENERAL")
                || hasAuthority("ROLE_PLATFORM_ADMIN");
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private String currentUserKey() {
        Long userId = authorizationService.currentUsuarioId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null
                ? "system"
                : authentication.getName();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private int progress(BigDecimal actual, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return actual != null && actual.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return actual.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(999))
                .intValue();
    }

    private int progress(long actual, Integer target) {
        return progress(BigDecimal.valueOf(actual), BigDecimal.valueOf(target == null ? 0 : target));
    }

    private record PeriodPerformance(Map<String, OwnerPerformance> byOwner, OwnerPerformance team) {
    }

    private record OwnerPerformance(
            BigDecimal revenue,
            long won,
            long closed,
            long prospects,
            long activities
    ) {
        private static OwnerPerformance empty() {
            return new OwnerPerformance(BigDecimal.ZERO, 0, 0, 0, 0);
        }
    }

    private static final class MutableOwnerPerformance {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long won;
        private long closed;
        private long prospects;
        private long activities;

        private void add(OwnerPerformance value) {
            revenue = revenue.add(value.revenue());
            won += value.won();
            closed += value.closed();
            prospects += value.prospects();
            activities += value.activities();
        }

        private OwnerPerformance toImmutable() {
            return new OwnerPerformance(
                    revenue.setScale(2, RoundingMode.HALF_UP), won, closed, prospects, activities);
        }
    }
}
