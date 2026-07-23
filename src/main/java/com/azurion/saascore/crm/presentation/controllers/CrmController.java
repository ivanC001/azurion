package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.crm.application.dto.CreateCrmActividadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmEtapaPipelineRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmNegociacionRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmProspectoRequest;
import com.azurion.saascore.crm.application.dto.CrmActividadResponse;
import com.azurion.saascore.crm.application.dto.CrmCanalTokenConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.CrmCurrencyConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmDashboardResponse;
import com.azurion.saascore.crm.application.dto.CrmInboxChannelResponse;
import com.azurion.saascore.crm.application.dto.CrmEtapaPipelineResponse;
import com.azurion.saascore.crm.application.dto.CrmNegociacionResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadHistorialResponse;
import com.azurion.saascore.crm.application.dto.CrmPipelineColumnResponse;
import com.azurion.saascore.crm.application.dto.CrmProspectoInteresResponse;
import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.application.dto.CrmLeadAssignmentConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmLandingConfigResponse;
import com.azurion.saascore.crm.application.dto.CrmReporteBucketResponse;
import com.azurion.saascore.crm.application.dto.CrmReportesResponse;
import com.azurion.saascore.crm.application.dto.CrmSentEmailResponse;
import com.azurion.saascore.crm.application.dto.GenerarCotizacionDesdeOportunidadRequest;
import com.azurion.saascore.crm.application.dto.MarcarPerdidaRequest;
import com.azurion.saascore.crm.application.dto.RealizarCrmActividadRequest;
import com.azurion.saascore.crm.application.dto.RepartirCrmProspectosRequest;
import com.azurion.saascore.crm.application.dto.RepartirCrmProspectosResponse;
import com.azurion.saascore.crm.application.dto.UpdateCrmCanalTokenConfigRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmCurrencyConfigRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmEtapaPipelineRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmOportunidadEtapaRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmProspectoRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmLeadAssignmentConfigRequest;
import com.azurion.saascore.crm.application.dto.SaveCrmLandingConfigRequest;
import com.azurion.saascore.crm.application.services.CrmLeadAssignmentService;
import com.azurion.saascore.crm.application.services.CrmLandingConfigurationService;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.saascore.settings.email.application.services.TenantEmailConfigService;
import com.azurion.shared.api.ApiResponse;
import com.azurion.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/saas/crm", "/crm"})
@RequiredArgsConstructor
@RequireModule("CRM")
public class CrmController {

    private final CrmUseCaseService crmUseCaseService;
    private final CrmLeadAssignmentService leadAssignmentService;
    private final CrmLandingConfigurationService landingConfigurationService;
    private final TenantEmailConfigService tenantEmailConfigService;

    @GetMapping("/configuracion/monedas")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<List<CrmCurrencyConfigResponse>> listCurrencyConfig() {
        return ApiResponse.ok(crmUseCaseService.listCurrencyConfig(), "Monedas CRM");
    }

    @PutMapping("/configuracion/monedas")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmCurrencyConfigResponse> saveCurrencyConfig(@Valid @RequestBody UpdateCrmCurrencyConfigRequest request) {
        return ApiResponse.ok(crmUseCaseService.saveCurrencyConfig(request), "Moneda CRM guardada");
    }

    @GetMapping("/integraciones")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<List<CrmCanalTokenConfigResponse>> listIntegraciones() {
        return ApiResponse.ok(crmUseCaseService.listCanalTokenConfig(), "Integraciones CRM");
    }

    @PutMapping("/integraciones")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmCanalTokenConfigResponse> saveIntegracion(@Valid @RequestBody UpdateCrmCanalTokenConfigRequest request) {
        return ApiResponse.ok(crmUseCaseService.saveCanalTokenConfig(request), "Integracion CRM guardada");
    }

    @GetMapping("/configuracion/landings")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<List<CrmLandingConfigResponse>> listLandingConfigurations() {
        return ApiResponse.ok(landingConfigurationService.list(), "Landings CRM configuradas");
    }

    @PostMapping("/configuracion/landings")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmLandingConfigResponse> createLandingConfiguration(
            @Valid @RequestBody SaveCrmLandingConfigRequest request) {
        return ApiResponse.ok(landingConfigurationService.create(request), "Landing CRM creada");
    }

    @PutMapping("/configuracion/landings/{id}")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmLandingConfigResponse> updateLandingConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody SaveCrmLandingConfigRequest request) {
        return ApiResponse.ok(landingConfigurationService.update(id, request), "Landing CRM actualizada");
    }

    @PostMapping("/configuracion/landings/{id}/regenerar-key")
    @PreAuthorize("hasAuthority('CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmLandingConfigResponse> regenerateLandingKey(@PathVariable Long id) {
        return ApiResponse.ok(
                landingConfigurationService.regenerateKey(id),
                "Landing key regenerada; actualiza las landings que usaban la clave anterior"
        );
    }

    @GetMapping("/bandeja/canales")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ','CRM_CONFIG_MANAGE')")
    public ApiResponse<List<CrmInboxChannelResponse>> listInboxChannels() {
        return ApiResponse.ok(
                crmUseCaseService.listInboxChannels(tenantEmailConfigService.isCurrentTenantEmailActive()),
                "Canales disponibles en la bandeja CRM"
        );
    }

    @GetMapping("/bandeja/correo/enviados")
    @PreAuthorize("hasAnyAuthority('CRM_LEADS_READ','CRM_ACTIVITIES_READ','CRM_CONFIG_MANAGE')")
    public ApiResponse<PageResponse<CrmSentEmailResponse>> pageSentEmails(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                crmUseCaseService.pageSentEmails(query, page, size),
                "Correos enviados desde el CRM"
        );
    }

    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyAuthority('CRM_CATALOG_MANAGE','CRM_LEADS_READ','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmCatalogoItemResponse>> listCatalogo(@RequestParam(required = false) String tipoItem) {
        return ApiResponse.ok(crmUseCaseService.listCatalogo(tipoItem), "Catalogo comercial CRM");
    }

    @PostMapping("/catalogo")
    @PreAuthorize("hasAnyAuthority('CRM_CATALOG_MANAGE','CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmCatalogoItemResponse> createCatalogo(@Valid @RequestBody CreateCrmCatalogoItemRequest request) {
        return ApiResponse.ok(crmUseCaseService.createCatalogoItem(request), "Item comercial CRM creado");
    }

    @PutMapping("/catalogo/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_CATALOG_MANAGE','CRM_CONFIG_MANAGE')")
    public ApiResponse<CrmCatalogoItemResponse> updateCatalogo(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateCrmCatalogoItemRequest request) {
        return ApiResponse.ok(crmUseCaseService.updateCatalogoItem(id, request), "Item comercial CRM actualizado");
    }

    @GetMapping("/etapas")
    @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_READ','CRM_PIPELINE_VIEW','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmEtapaPipelineResponse>> listEtapas() {
        return ApiResponse.ok(crmUseCaseService.listEtapas(), "Etapas del embudo CRM");
    }

    @PostMapping("/etapas")
    @PreAuthorize("hasAnyAuthority('CRM_CONFIG_MANAGE','CRM_PIPELINE_WRITE','CRM_PIPELINE_MANAGE')")
    public ApiResponse<CrmEtapaPipelineResponse> createEtapa(@Valid @RequestBody CreateCrmEtapaPipelineRequest request) {
        return ApiResponse.ok(crmUseCaseService.createEtapa(request), "Etapa CRM creada");
    }

    @PutMapping("/etapas/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_CONFIG_MANAGE','CRM_PIPELINE_WRITE','CRM_PIPELINE_MANAGE')")
    public ApiResponse<CrmEtapaPipelineResponse> updateEtapa(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateCrmEtapaPipelineRequest request) {
        return ApiResponse.ok(crmUseCaseService.updateEtapa(id, request), "Etapa CRM actualizada");
    }

    @GetMapping("/pipeline")
    @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_READ','CRM_PIPELINE_VIEW','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmPipelineColumnResponse>> pipeline() {
        return ApiResponse.ok(crmUseCaseService.pipeline(), "Pipeline CRM");
    }

    @PostMapping("/prospectos")
    @PreAuthorize("hasAuthority('CRM_LEADS_WRITE')")
    public ApiResponse<CrmProspectoResponse> createProspecto(@Valid @RequestBody CreateCrmProspectoRequest request) {
        return ApiResponse.ok(crmUseCaseService.createProspecto(request), "Prospecto CRM creado");
    }

    @GetMapping("/prospectos")
    @PreAuthorize("hasAuthority('CRM_LEADS_READ')")
    public ApiResponse<List<CrmProspectoResponse>> listProspectos() {
        return ApiResponse.ok(crmUseCaseService.listProspectos(), "Prospectos CRM");
    }

    @GetMapping("/prospectos/page")
    @PreAuthorize("hasAuthority('CRM_LEADS_READ')")
    public ApiResponse<PageResponse<CrmProspectoResponse>> pageProspectos(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String canalIngreso,
            @RequestParam(required = false) String campania,
            @RequestParam(required = false) String responsableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                crmUseCaseService.pageProspectos(query, estado, origen, canalIngreso, campania, responsableId, fechaDesde, fechaHasta, page, size),
                "Prospectos CRM paginados"
        );
    }

    @GetMapping("/prospectos/{id}")
    @PreAuthorize("hasAuthority('CRM_LEADS_READ')")
    public ApiResponse<CrmProspectoResponse> getProspecto(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.getProspecto(id), "Prospecto CRM");
    }

    @GetMapping("/prospectos/{id}/intereses")
    @PreAuthorize("hasAuthority('CRM_LEADS_READ')")
    public ApiResponse<List<CrmProspectoInteresResponse>> listProspectoIntereses(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.listProspectoIntereses(id), "Intereses del prospecto CRM");
    }

    @PutMapping("/prospectos/{id}")
    @PreAuthorize("hasAuthority('CRM_LEADS_WRITE')")
    public ApiResponse<CrmProspectoResponse> updateProspecto(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateCrmProspectoRequest request) {
        return ApiResponse.ok(crmUseCaseService.updateProspecto(id, request), "Prospecto CRM actualizado");
    }

    @PostMapping("/prospectos/repartir")
    @PreAuthorize("hasAnyAuthority('CRM_ASSIGN','CRM_VIEW_ALL','ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public ApiResponse<RepartirCrmProspectosResponse> repartirProspectos(@Valid @RequestBody RepartirCrmProspectosRequest request) {
        return ApiResponse.ok(crmUseCaseService.repartirProspectos(request), "Prospectos CRM repartidos");
    }

    @GetMapping("/prospectos/reparto-configuracion")
    @PreAuthorize("hasAnyAuthority('CRM_ASSIGN','CRM_VIEW_ALL','ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public ApiResponse<CrmLeadAssignmentConfigResponse> getLeadAssignmentConfiguration() {
        return ApiResponse.ok(leadAssignmentService.getConfiguration(), "Configuracion de reparto de leads");
    }

    @PutMapping("/prospectos/reparto-configuracion")
    @PreAuthorize("hasAnyAuthority('CRM_ASSIGN','CRM_VIEW_ALL','ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public ApiResponse<CrmLeadAssignmentConfigResponse> updateLeadAssignmentConfiguration(
            @Valid @RequestBody UpdateCrmLeadAssignmentConfigRequest request) {
        return ApiResponse.ok(leadAssignmentService.updateConfiguration(request), "Configuracion de reparto actualizada");
    }

    @DeleteMapping("/prospectos/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_DELETE','ROLE_ADMIN_GENERAL','ROLE_PLATFORM_ADMIN')")
    public ApiResponse<Void> deleteProspecto(@PathVariable Long id) {
        crmUseCaseService.deleteProspecto(id);
        return ApiResponse.ok(null, "Prospecto CRM eliminado");
    }

    @PostMapping("/prospectos/{id}/convertir-cliente")
    @PreAuthorize("hasAnyAuthority('CRM_CONVERT_CLIENT','CRM_PROSPECTS_CONVERT')")
    public ApiResponse<ClienteResponse> convertirCliente(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.convertirProspectoCliente(id), "Prospecto convertido a cliente");
    }

    @PostMapping("/oportunidades")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_WRITE')")
    public ApiResponse<CrmOportunidadResponse> createOportunidad(@Valid @RequestBody CreateCrmOportunidadRequest request) {
        return ApiResponse.ok(crmUseCaseService.createOportunidad(request), "Oportunidad CRM creada");
    }

    @GetMapping("/oportunidades")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmOportunidadResponse>> listOportunidades() {
        return ApiResponse.ok(crmUseCaseService.listOportunidades(), "Oportunidades CRM");
    }

    @GetMapping("/oportunidades/page")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<PageResponse<CrmOportunidadResponse>> pageOportunidades(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long etapaId,
            @RequestParam(required = false) String etapa,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String responsableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                crmUseCaseService.pageOportunidades(query, etapaId, etapa, estado, responsableId, cierreDesde, cierreHasta, page, size),
                "Oportunidades CRM paginadas"
        );
    }

    @GetMapping("/resultados/page")
    @PreAuthorize("hasAnyAuthority('CRM_OPPORTUNITIES_READ','CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<PageResponse<CrmOportunidadResponse>> pageResultados(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String responsableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                crmUseCaseService.pageResultados(query, estado, responsableId, cierreDesde, cierreHasta, page, size),
                "Resultados comerciales CRM paginados"
        );
    }

    @GetMapping("/oportunidades/{id:\\d+}")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<CrmOportunidadResponse> getOportunidad(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.getOportunidad(id), "Oportunidad CRM");
    }

    @GetMapping("/oportunidades/{id}/historial")
    @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_READ','CRM_PIPELINE_VIEW','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmOportunidadHistorialResponse>> historialOportunidad(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.historialOportunidad(id), "Historial de oportunidad CRM");
    }

    @PutMapping("/oportunidades/{id}")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_WRITE')")
    public ApiResponse<CrmOportunidadResponse> updateOportunidad(@PathVariable Long id,
                                                                 @Valid @RequestBody UpdateCrmOportunidadRequest request) {
        return ApiResponse.ok(crmUseCaseService.updateOportunidad(id, request), "Oportunidad CRM actualizada");
    }

    @PutMapping("/oportunidades/{id}/etapa")
    @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_WRITE','CRM_OPPORTUNITIES_STAGE','CRM_OPPORTUNITY_MOVE_STAGE')")
    public ApiResponse<CrmOportunidadResponse> moverEtapa(@PathVariable Long id,
                                                          @Valid @RequestBody UpdateCrmOportunidadEtapaRequest request) {
        return ApiResponse.ok(crmUseCaseService.moverOportunidadEtapa(id, request), "Etapa de oportunidad actualizada");
    }

    @PostMapping("/oportunidades/{id}/marcar-ganada")
    @PreAuthorize("hasAnyAuthority('CRM_CONVERT_SALE','CRM_OPPORTUNITIES_CLOSE','CRM_OPPORTUNITY_MARK_WON')")
    public ApiResponse<CrmOportunidadResponse> marcarGanada(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.marcarGanada(id), "Oportunidad marcada como ganada");
    }

    @PostMapping("/oportunidades/{id}/marcar-perdida")
    @PreAuthorize("hasAnyAuthority('CRM_OPPORTUNITIES_CLOSE','CRM_OPPORTUNITY_MARK_LOST')")
    public ApiResponse<CrmOportunidadResponse> marcarPerdida(@PathVariable Long id,
                                                             @Valid @RequestBody MarcarPerdidaRequest request) {
        return ApiResponse.ok(crmUseCaseService.marcarPerdida(id, request), "Oportunidad marcada como perdida");
    }

    @PostMapping("/oportunidades/{id}/generar-cotizacion")
    @PreAuthorize("hasAnyAuthority('CRM_CONVERT_SALE','CRM_QUOTES_CREATE')")
    public ApiResponse<CotizacionResponse> generarCotizacion(@PathVariable Long id,
                                                             @Valid @RequestBody GenerarCotizacionDesdeOportunidadRequest request) {
        return ApiResponse.ok(crmUseCaseService.generarCotizacion(id, request), "Cotizacion generada desde CRM");
    }

    @GetMapping("/oportunidades/{id}/negociaciones")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmNegociacionResponse>> listNegociaciones(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.listNegociaciones(id), "Negociaciones de la oportunidad");
    }

    @PostMapping("/oportunidades/{id}/negociaciones")
    @PreAuthorize("hasAnyAuthority('CRM_OPPORTUNITIES_WRITE','CRM_OPPORTUNITIES_STAGE')")
    public ApiResponse<CrmNegociacionResponse> registrarNegociacion(@PathVariable Long id,
                                                                    @Valid @RequestBody CreateCrmNegociacionRequest request) {
        return ApiResponse.ok(crmUseCaseService.registrarNegociacion(id, request), "Negociacion registrada");
    }

    @PostMapping("/actividades")
    @PreAuthorize("hasAuthority('CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmActividadResponse> createActividad(@Valid @RequestBody CreateCrmActividadRequest request) {
        return ApiResponse.ok(crmUseCaseService.createActividad(request), "Actividad CRM creada");
    }

    @GetMapping("/actividades")
    @PreAuthorize("hasAuthority('CRM_ACTIVITIES_READ')")
    public ApiResponse<List<CrmActividadResponse>> listActividades() {
        return ApiResponse.ok(crmUseCaseService.listActividades(), "Actividades CRM");
    }

    @GetMapping("/actividades/page")
    @PreAuthorize("hasAuthority('CRM_ACTIVITIES_READ')")
    public ApiResponse<PageResponse<CrmActividadResponse>> pageActividades(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoActividad,
            @RequestParam(required = false) String usuarioId,
            @RequestParam(required = false) Long prospectoId,
            @RequestParam(required = false) Long oportunidadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                crmUseCaseService.pageActividades(query, estado, tipoActividad, usuarioId, prospectoId, oportunidadId, fechaDesde, fechaHasta, page, size),
                "Actividades CRM paginadas"
        );
    }

    @GetMapping("/pagos/seguimiento/page")
    @PreAuthorize("hasAuthority('CRM_OPPORTUNITIES_READ')")
    public ApiResponse<PageResponse<CrmOportunidadResponse>> pageSeguimientoPagos(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String responsableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                crmUseCaseService.pageSeguimientoPagos(query, responsableId, cierreDesde, cierreHasta, page, size),
                "Seguimiento de pagos CRM paginado"
        );
    }

    @GetMapping("/actividades/{id}")
    @PreAuthorize("hasAuthority('CRM_ACTIVITIES_READ')")
    public ApiResponse<CrmActividadResponse> getActividad(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.getActividad(id), "Actividad CRM");
    }

    @PutMapping("/actividades/{id}/realizar")
    @PreAuthorize("hasAuthority('CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmActividadResponse> realizarActividad(@PathVariable Long id,
                                                               @Valid @RequestBody RealizarCrmActividadRequest request) {
        return ApiResponse.ok(crmUseCaseService.realizarActividad(id, request), "Actividad CRM realizada");
    }

    @PutMapping("/actividades/{id}/cancelar")
    @PreAuthorize("hasAuthority('CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmActividadResponse> cancelarActividad(@PathVariable Long id,
                                                               @Valid @RequestBody RealizarCrmActividadRequest request) {
        return ApiResponse.ok(crmUseCaseService.cancelarActividad(id, request), "Actividad CRM cancelada");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<CrmDashboardResponse> dashboard() {
        return ApiResponse.ok(crmUseCaseService.dashboard(), "Dashboard CRM");
    }

    @GetMapping("/reportes")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<CrmReportesResponse> reportes() {
        return ApiResponse.ok(crmUseCaseService.reportes(), "Reportes CRM");
    }

    @GetMapping("/reportes/oportunidades-etapa")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<List<CrmReporteBucketResponse>> oportunidadesEtapa() {
        return ApiResponse.ok(crmUseCaseService.reporteOportunidadesEtapa(), "Oportunidades por etapa");
    }

    @GetMapping("/reportes/oportunidades-vendedor")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<List<CrmReporteBucketResponse>> oportunidadesVendedor() {
        return ApiResponse.ok(crmUseCaseService.reporteOportunidadesVendedor(), "Oportunidades por vendedor");
    }

    @GetMapping("/reportes/conversiones")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<Map<String, Object>> conversiones() {
        return ApiResponse.ok(crmUseCaseService.reporteConversiones(), "Conversiones CRM");
    }

    @GetMapping("/reportes/prospectos-origen")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<List<CrmReporteBucketResponse>> prospectosOrigen() {
        return ApiResponse.ok(crmUseCaseService.reporteProspectosOrigen(), "Prospectos por origen");
    }

    @GetMapping("/reportes/ganadas-perdidas")
    @PreAuthorize("hasAnyAuthority('CRM_REPORTS_READ','CRM_REPORTS_TEAM')")
    public ApiResponse<Map<String, Object>> ganadasPerdidas() {
        return ApiResponse.ok(crmUseCaseService.reporteGanadasPerdidas(), "Ganadas y perdidas CRM");
    }
}
