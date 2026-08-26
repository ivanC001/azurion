package com.azurion.saascore.crm.application.usecases;

import static com.azurion.saascore.crm.application.support.CrmSupport.hasText;
import static com.azurion.saascore.crm.application.support.CrmSupport.money;
import static com.azurion.saascore.crm.application.support.CrmSupport.trim;

import com.azurion.saascore.crm.application.dto.CrmDashboardResponse;
import com.azurion.saascore.crm.application.dto.CrmEtapaResumenResponse;
import com.azurion.saascore.crm.application.dto.CrmReporteBucketResponse;
import com.azurion.saascore.crm.application.dto.CrmReportesResponse;
import com.azurion.saascore.crm.application.dto.CrmResponsableOptionResponse;
import com.azurion.saascore.crm.application.support.CrmAccessPolicy;
import com.azurion.saascore.crm.application.support.CrmCurrencyConverter;
import com.azurion.saascore.crm.application.support.CrmSupport;
import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmEtapaPipelineRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard y reportes agregados del CRM.
 *
 * Extraido de CrmUseCaseService: es logica de solo lectura que no muta ninguna
 * entidad, asi que separarla deja el servicio principal centrado en los flujos
 * que si cambian el estado del CRM.
 *
 * Todas las consultas se acotan al ambito del usuario (CrmAccessPolicy): quien
 * no puede ver todo el CRM solo agrega sus propios registros.
 */
@Service
@RequiredArgsConstructor
public class CrmReporteUseCase {

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_GANADA = "GANADA";
    private static final String ESTADO_PERDIDA = "PERDIDA";

    private final CrmProspectoRepository prospectoRepository;
    private final CrmOportunidadRepository oportunidadRepository;
    private final CrmActividadRepository actividadRepository;
    private final CrmEtapaPipelineRepository etapaPipelineRepository;
    private final UsuarioTenantRepository usuarioTenantRepository;
    private final CrmAccessPolicy accessPolicy;
    private final CrmCurrencyConverter currencyConverter;

    @Transactional(readOnly = true)
    public CrmDashboardResponse dashboard() {
        boolean viewAll = accessPolicy.canViewAll();
        String current = accessPolicy.currentUserKey();
        String ownerScope = viewAll ? null : current;
        return new CrmDashboardResponse(
                viewAll ? prospectoRepository.countByEstado("NUEVO") : prospectoRepository.countByResponsableIdAndEstado(current, "NUEVO"),
                viewAll ? prospectoRepository.countByEstado("CONVERTIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "CONVERTIDO"),
                viewAll ? oportunidadRepository.countByEstado("ABIERTA") : oportunidadRepository.countByResponsableIdAndEstado(current, "ABIERTA"),
                viewAll ? oportunidadRepository.countByEstado(ESTADO_GANADA) : oportunidadRepository.countByResponsableIdAndEstado(current, ESTADO_GANADA),
                viewAll ? oportunidadRepository.countByEstado(ESTADO_PERDIDA) : oportunidadRepository.countByResponsableIdAndEstado(current, ESTADO_PERDIDA),
                viewAll ? actividadRepository.countByEstado(ESTADO_PENDIENTE) : actividadRepository.countByUsuarioIdAndEstado(current, ESTADO_PENDIENTE),
                viewAll
                        ? actividadRepository.countByEstadoAndFechaProgramadaBefore(ESTADO_PENDIENTE, OffsetDateTime.now())
                        : actividadRepository.countByUsuarioIdAndEstadoAndFechaProgramadaBefore(current, ESTADO_PENDIENTE, OffsetDateTime.now()),
                viewAll ? prospectoRepository.countByCanalIngresoNot("MANUAL") : 0,
                viewAll ? prospectoRepository.countByCanalIngreso("MANUAL") : 0,
                currencyConverter.sumCurrencyAmounts(oportunidadRepository.sumOpenPipelineScoped(ownerScope)),
                resumenPorEtapaScoped(ownerScope)
        );
    }

    @Transactional(readOnly = true)
    public CrmReportesResponse reportes() {
        boolean viewAll = accessPolicy.canViewAll();
        String current = accessPolicy.currentUserKey();
        return new CrmReportesResponse(
                resumenPorEtapaScoped(viewAll ? null : current),
                viewAll ? actividadRepository.countByEstado(ESTADO_PENDIENTE) : actividadRepository.countByUsuarioIdAndEstado(current, ESTADO_PENDIENTE),
                viewAll ? actividadRepository.countByEstado("REALIZADA") : actividadRepository.countByUsuarioIdAndEstado(current, "REALIZADA"),
                viewAll ? prospectoRepository.countByEstado("CONVERTIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "CONVERTIDO"),
                viewAll ? prospectoRepository.countByEstado("PERDIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "PERDIDO")
        );
    }

    @Transactional(readOnly = true)
    public List<CrmReporteBucketResponse> reporteOportunidadesEtapa() {
        return resumenPorEtapaScoped(accessPolicy.ownerScope()).stream()
                .map(item -> new CrmReporteBucketResponse(item.etapa(), item.etapa(), item.cantidad(), item.monto()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmReporteBucketResponse> reporteOportunidadesVendedor() {
        String ownerScope = accessPolicy.ownerScope();
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (CrmOportunidadRepository.AggregateProjection row
                : oportunidadRepository.summarizeByOwnerScoped(ownerScope)) {
            counts.merge(row.getCodigo(), row.getCantidad(), Long::sum);
            amounts.merge(
                    row.getCodigo(),
                    currencyConverter.toTenantBase(row.getMonto(), row.getMoneda()),
                    BigDecimal::add
            );
        }
        return counts.entrySet().stream()
                .map(entry -> new CrmReporteBucketResponse(
                        entry.getKey(),
                        entry.getKey(),
                        entry.getValue(),
                        money(amounts.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reporteConversiones() {
        boolean viewAll = accessPolicy.canViewAll();
        String current = accessPolicy.currentUserKey();
        String ownerScope = viewAll ? null : current;
        long prospectos = viewAll ? prospectoRepository.count() : prospectoRepository.countByResponsableId(current);
        long convertidos = viewAll ? prospectoRepository.countByEstado("CONVERTIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "CONVERTIDO");
        long oportunidades = oportunidadRepository.countScoped(ownerScope);
        long ganadas = viewAll
                ? oportunidadRepository.countByEstado(ESTADO_GANADA)
                : oportunidadRepository.countByResponsableIdAndEstado(current, ESTADO_GANADA);
        long cotizadas = oportunidadRepository.countQuotedScoped(ownerScope);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("prospectoCliente", conversion(convertidos, prospectos));
        response.put("oportunidadGanada", conversion(ganadas, oportunidades));
        response.put("cotizacionVenta", conversion(ganadas, cotizadas));
        response.put("prospectos", prospectos);
        response.put("prospectosConvertidos", convertidos);
        response.put("oportunidades", oportunidades);
        response.put("oportunidadesGanadas", ganadas);
        return response;
    }

    @Transactional(readOnly = true)
    public List<CrmReporteBucketResponse> reporteProspectosOrigen() {
        return prospectoRepository.summarizeByOriginScoped(accessPolicy.ownerScope()).stream()
                .map(row -> new CrmReporteBucketResponse(
                        row.getCodigo(), row.getCodigo(), row.getCantidad(), BigDecimal.ZERO
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmResponsableOptionResponse> reporteResponsables() {
        return usuarioTenantRepository.findAllByOrderByNombresAsc().stream()
                .map(usuario -> new CrmResponsableOptionResponse(
                        String.valueOf(usuario.getId()),
                        usuario.getUsername(),
                        nombreCompleto(usuario)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reporteGanadasPerdidas() {
        boolean viewAll = accessPolicy.canViewAll();
        String current = accessPolicy.currentUserKey();
        String ownerScope = viewAll ? null : current;
        long ganadas = viewAll
                ? oportunidadRepository.countByEstado(ESTADO_GANADA)
                : oportunidadRepository.countByResponsableIdAndEstado(current, ESTADO_GANADA);
        long perdidas = viewAll
                ? oportunidadRepository.countByEstado(ESTADO_PERDIDA)
                : oportunidadRepository.countByResponsableIdAndEstado(current, ESTADO_PERDIDA);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ganadas", ganadas);
        response.put("perdidas", perdidas);
        response.put("montoGanado", currencyConverter.sumCurrencyAmounts(oportunidadRepository.sumRealByEstadoScoped(ownerScope, ESTADO_GANADA)));
        response.put("montoPerdido", currencyConverter.sumCurrencyAmounts(oportunidadRepository.sumRealByEstadoScoped(ownerScope, ESTADO_PERDIDA)));
        return response;
    }

    /**
     * Importes de cada etapa consolidados en la moneda base del tenant.
     */
    private List<CrmEtapaResumenResponse> resumenPorEtapaScoped(String ownerScope) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (CrmOportunidadRepository.AggregateProjection row
                : oportunidadRepository.summarizeByStageScoped(ownerScope)) {
            counts.merge(row.getCodigo(), row.getCantidad(), Long::sum);
            totals.merge(
                    row.getCodigo(),
                    currencyConverter.toTenantBase(row.getMonto(), row.getMoneda()),
                    BigDecimal::add
            );
        }
        return activeStages().stream()
                .map(stage -> new CrmEtapaResumenResponse(
                        stage.getCodigo(),
                        counts.getOrDefault(stage.getCodigo(), 0L),
                        money(totals.getOrDefault(stage.getCodigo(), BigDecimal.ZERO))
                ))
                .toList();
    }

    private List<CrmEtapaPipeline> activeStages() {
        return etapaPipelineRepository.findByActivoTrueOrderByOrdenAscIdAsc();
    }

    /**
     * Porcentaje de conversion; un denominador vacio devuelve cero en lugar de
     * romper la division.
     */
    private BigDecimal conversion(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String nombreCompleto(UsuarioTenant usuario) {
        String nombre = Stream.of(trim(usuario.getNombres()), trim(usuario.getApellidos()))
                .filter(CrmSupport::hasText)
                .collect(Collectors.joining(" "));
        return hasText(nombre) ? nombre : usuario.getUsername();
    }
}
