package com.azurion.saascore.crm.presentation.controllers;

import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.crm.application.dto.CreateCrmActividadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmEtapaPipelineRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmProspectoRequest;
import com.azurion.saascore.crm.application.dto.CrmActividadResponse;
import com.azurion.saascore.crm.application.dto.CrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.CrmDashboardResponse;
import com.azurion.saascore.crm.application.dto.CrmEtapaPipelineResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadHistorialResponse;
import com.azurion.saascore.crm.application.dto.CrmPipelineColumnResponse;
import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.application.dto.CrmReporteBucketResponse;
import com.azurion.saascore.crm.application.dto.CrmReportesResponse;
import com.azurion.saascore.crm.application.dto.GenerarCotizacionDesdeOportunidadRequest;
import com.azurion.saascore.crm.application.dto.MarcarPerdidaRequest;
import com.azurion.saascore.crm.application.dto.RealizarCrmActividadRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmEtapaPipelineRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmOportunidadEtapaRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmProspectoRequest;
import com.azurion.saascore.crm.application.usecases.CrmUseCaseService;
import com.azurion.saascore.modulos.application.services.RequireModule;
import com.azurion.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_CATALOG_MANAGE')")
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
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_LEADS_WRITE')")
    public ApiResponse<CrmProspectoResponse> createProspecto(@Valid @RequestBody CreateCrmProspectoRequest request) {
        return ApiResponse.ok(crmUseCaseService.createProspecto(request), "Prospecto CRM creado");
    }

    @GetMapping("/prospectos")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_LEADS_READ')")
    public ApiResponse<List<CrmProspectoResponse>> listProspectos() {
        return ApiResponse.ok(crmUseCaseService.listProspectos(), "Prospectos CRM");
    }

    @GetMapping("/prospectos/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_LEADS_READ')")
    public ApiResponse<CrmProspectoResponse> getProspecto(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.getProspecto(id), "Prospecto CRM");
    }

    @PutMapping("/prospectos/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_LEADS_WRITE')")
    public ApiResponse<CrmProspectoResponse> updateProspecto(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateCrmProspectoRequest request) {
        return ApiResponse.ok(crmUseCaseService.updateProspecto(id, request), "Prospecto CRM actualizado");
    }

    @PostMapping("/prospectos/{id}/convertir-cliente")
    @PreAuthorize("hasAnyAuthority('CRM_CONVERT_CLIENT','CRM_PROSPECTS_CONVERT')")
    public ApiResponse<ClienteResponse> convertirCliente(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.convertirProspectoCliente(id), "Prospecto convertido a cliente");
    }

    @PostMapping("/oportunidades")
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_OPPORTUNITIES_WRITE')")
    public ApiResponse<CrmOportunidadResponse> createOportunidad(@Valid @RequestBody CreateCrmOportunidadRequest request) {
        return ApiResponse.ok(crmUseCaseService.createOportunidad(request), "Oportunidad CRM creada");
    }

    @GetMapping("/oportunidades")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmOportunidadResponse>> listOportunidades() {
        return ApiResponse.ok(crmUseCaseService.listOportunidades(), "Oportunidades CRM");
    }

    @GetMapping("/oportunidades/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<CrmOportunidadResponse> getOportunidad(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.getOportunidad(id), "Oportunidad CRM");
    }

    @GetMapping("/oportunidades/{id}/historial")
    @PreAuthorize("hasAnyAuthority('CRM_PIPELINE_READ','CRM_PIPELINE_VIEW','CRM_OPPORTUNITIES_READ')")
    public ApiResponse<List<CrmOportunidadHistorialResponse>> historialOportunidad(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.historialOportunidad(id), "Historial de oportunidad CRM");
    }

    @PutMapping("/oportunidades/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_OPPORTUNITIES_WRITE')")
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
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_OPPORTUNITIES_CLOSE','CRM_OPPORTUNITY_MARK_LOST')")
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

    @PostMapping("/actividades")
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmActividadResponse> createActividad(@Valid @RequestBody CreateCrmActividadRequest request) {
        return ApiResponse.ok(crmUseCaseService.createActividad(request), "Actividad CRM creada");
    }

    @GetMapping("/actividades")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_ACTIVITIES_READ')")
    public ApiResponse<List<CrmActividadResponse>> listActividades() {
        return ApiResponse.ok(crmUseCaseService.listActividades(), "Actividades CRM");
    }

    @GetMapping("/actividades/{id}")
    @PreAuthorize("hasAnyAuthority('CRM_READ','CRM_ACTIVITIES_READ')")
    public ApiResponse<CrmActividadResponse> getActividad(@PathVariable Long id) {
        return ApiResponse.ok(crmUseCaseService.getActividad(id), "Actividad CRM");
    }

    @PutMapping("/actividades/{id}/realizar")
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_ACTIVITIES_WRITE')")
    public ApiResponse<CrmActividadResponse> realizarActividad(@PathVariable Long id,
                                                               @Valid @RequestBody RealizarCrmActividadRequest request) {
        return ApiResponse.ok(crmUseCaseService.realizarActividad(id, request), "Actividad CRM realizada");
    }

    @PutMapping("/actividades/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('CRM_WRITE','CRM_ACTIVITIES_WRITE')")
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
