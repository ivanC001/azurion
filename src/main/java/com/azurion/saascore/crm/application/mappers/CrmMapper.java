package com.azurion.saascore.crm.application.mappers;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.crm.application.dto.CrmActividadResponse;
import com.azurion.saascore.crm.application.dto.CrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.CrmEtapaPipelineResponse;
import com.azurion.saascore.crm.application.dto.CrmNegociacionResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadHistorialResponse;
import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.domain.entities.CrmActividad;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import com.azurion.saascore.crm.domain.entities.CrmNegociacion;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmOportunidadHistorial;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import java.util.List;

public final class CrmMapper {

    private CrmMapper() {
    }

    public static CrmProspectoResponse toProspectoResponse(CrmProspecto prospecto) {
        return new CrmProspectoResponse(
                prospecto.getId(),
                prospecto.getTipoPersona(),
                prospecto.getTipoDocumento(),
                prospecto.getNumeroDocumento(),
                prospecto.getNombre(),
                prospecto.getRazonSocial(),
                prospecto.getNombreComercial(),
                prospecto.getTelefono(),
                prospecto.getCorreo(),
                prospecto.getDireccion(),
                prospecto.getOrigen(),
                prospecto.getCanalIngreso(),
                prospecto.getCampania(),
                prospecto.getLandingUrl(),
                prospecto.getMensaje(),
                prospecto.getTipoInteres(),
                prospecto.getInteresPrincipal(),
                prospecto.getInteresDetalle(),
                prospecto.getPresupuestoEstimado(),
                prospecto.getFechaInteres(),
                prospecto.getCatalogoItemId(),
                prospecto.getMetadataJson(),
                prospecto.getEstado(),
                prospecto.getNivelInteres(),
                prospecto.isNecesidadIdentificada(),
                prospecto.getInteresReal(),
                prospecto.getPresupuestoDefinido(),
                prospecto.getTomadorDecision(),
                prospecto.getFechaEstimadaCompra(),
                prospecto.getScoreCalificacion(),
                prospecto.getTemperatura(),
                prospecto.getMotivoEspera(),
                prospecto.getFechaProximoContacto(),
                prospecto.getMotivoPerdida(),
                prospecto.getObservacionPerdida(),
                prospecto.getOportunidadId(),
                prospecto.getResponsableId(),
                prospecto.getObservacion(),
                prospecto.getClienteId(),
                prospecto.getFechaConversion(),
                prospecto.getCreatedAt(),
                prospecto.getUpdatedAt()
        );
    }

    public static CrmOportunidadResponse toOportunidadResponse(CrmOportunidad oportunidad) {
        CrmProspecto prospecto = oportunidad.getProspecto();
        Cliente cliente = oportunidad.getCliente();
        CrmEtapaPipeline etapa = oportunidad.getEtapaPipeline();
        return new CrmOportunidadResponse(
                oportunidad.getId(),
                prospecto == null ? null : prospecto.getId(),
                prospecto == null ? null : prospecto.getNombre(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getNombre(),
                oportunidad.getTipoOportunidad(),
                oportunidad.getCatalogoItemId(),
                oportunidad.getTitulo(),
                oportunidad.getDescripcion(),
                oportunidad.getMontoEstimado(),
                oportunidad.getMontoReal(),
                oportunidad.getProbabilidad(),
                etapa == null ? null : etapa.getId(),
                oportunidad.getEtapa(),
                etapa == null ? oportunidad.getEtapa() : etapa.getNombre(),
                etapa == null ? null : etapa.getColor(),
                oportunidad.getFechaCierreEstimada(),
                oportunidad.getResponsableId(),
                oportunidad.getEstado(),
                oportunidad.getMotivoPerdida(),
                oportunidad.getFechaCierreReal(),
                oportunidad.getFechaUltimaActualizacion(),
                oportunidad.getFechaGanada(),
                oportunidad.getFechaPerdida(),
                oportunidad.getCreatedAt(),
                oportunidad.getUpdatedAt()
        );
    }

    public static CrmActividadResponse toActividadResponse(CrmActividad actividad) {
        CrmProspecto prospecto = actividad.getProspecto();
        CrmOportunidad oportunidad = actividad.getOportunidad();
        Cliente cliente = actividad.getCliente();
        return new CrmActividadResponse(
                actividad.getId(),
                prospecto == null ? null : prospecto.getId(),
                prospecto == null ? null : prospecto.getNombre(),
                oportunidad == null ? null : oportunidad.getId(),
                oportunidad == null ? null : oportunidad.getTitulo(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getNombre(),
                actividad.getTipoActividad(),
                actividad.getAsunto(),
                actividad.getDescripcion(),
                actividad.getFechaProgramada(),
                actividad.getFechaRealizada(),
                actividad.getEstado(),
                actividad.getUsuarioId(),
                actividad.getResultado(),
                actividad.getResultadoContacto(),
                actividad.getNivelInteres(),
                actividad.getEstadoProspectoResultado(),
                actividad.getCreatedAt(),
                actividad.getUpdatedAt()
        );
    }

    public static List<CrmProspectoResponse> toProspectoResponses(List<CrmProspecto> prospectos) {
        return prospectos.stream().map(CrmMapper::toProspectoResponse).toList();
    }

    public static List<CrmOportunidadResponse> toOportunidadResponses(List<CrmOportunidad> oportunidades) {
        return oportunidades.stream().map(CrmMapper::toOportunidadResponse).toList();
    }

    public static List<CrmActividadResponse> toActividadResponses(List<CrmActividad> actividades) {
        return actividades.stream().map(CrmMapper::toActividadResponse).toList();
    }

    public static CrmNegociacionResponse toNegociacionResponse(CrmNegociacion negociacion) {
        Cotizacion cotizacion = negociacion.getCotizacion();
        return new CrmNegociacionResponse(
                negociacion.getId(),
                negociacion.getOportunidad().getId(),
                cotizacion == null ? null : cotizacion.getId(),
                cotizacion == null ? null : "COT-" + String.format("%04d", cotizacion.getId()),
                negociacion.getEstado(),
                negociacion.getSolicitudCliente(),
                negociacion.getPrecioOriginal(),
                negociacion.getDescuento(),
                negociacion.getPrecioFinal(),
                negociacion.getFormaPago(),
                negociacion.getCuotas(),
                negociacion.getFechaInicio(),
                negociacion.getFechaEntrega(),
                negociacion.getObservacion(),
                negociacion.getResultado(),
                negociacion.getUsuarioId(),
                negociacion.getUsuarioNombre(),
                negociacion.getCreatedAt(),
                negociacion.getUpdatedAt()
        );
    }

    public static List<CrmNegociacionResponse> toNegociacionResponses(List<CrmNegociacion> negociaciones) {
        return negociaciones.stream().map(CrmMapper::toNegociacionResponse).toList();
    }

    public static CrmCatalogoItemResponse toCatalogoItemResponse(CrmCatalogoItem item) {
        return new CrmCatalogoItemResponse(
                item.getId(),
                item.getTipoItem(),
                item.getNombre(),
                item.getDescripcion(),
                item.getPrecioReferencial(),
                item.getEstado(),
                item.getMetadataJson(),
                item.getPublicToken(),
                item.isPublicEnabled(),
                item.getLandingSlug(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public static List<CrmCatalogoItemResponse> toCatalogoItemResponses(List<CrmCatalogoItem> items) {
        return items.stream().map(CrmMapper::toCatalogoItemResponse).toList();
    }

    public static CrmEtapaPipelineResponse toEtapaResponse(CrmEtapaPipeline etapa) {
        return new CrmEtapaPipelineResponse(
                etapa.getId(),
                etapa.getCodigo(),
                etapa.getNombre(),
                etapa.getDescripcion(),
                etapa.getOrden(),
                etapa.getProbabilidadDefault(),
                etapa.getColor(),
                etapa.getIcono(),
                etapa.isGanado(),
                etapa.isPerdido(),
                etapa.isRequiereValidacion(),
                etapa.getModoValidacion(),
                etapa.isActivo()
        );
    }

    public static List<CrmEtapaPipelineResponse> toEtapaResponses(List<CrmEtapaPipeline> etapas) {
        return etapas.stream().map(CrmMapper::toEtapaResponse).toList();
    }

    public static CrmOportunidadHistorialResponse toHistorialResponse(CrmOportunidadHistorial historial) {
        CrmEtapaPipeline origen = historial.getEtapaOrigen();
        CrmEtapaPipeline destino = historial.getEtapaDestino();
        return new CrmOportunidadHistorialResponse(
                historial.getId(),
                historial.getOportunidad().getId(),
                origen == null ? null : origen.getId(),
                origen == null ? null : origen.getCodigo(),
                origen == null ? null : origen.getNombre(),
                destino.getId(),
                destino.getCodigo(),
                destino.getNombre(),
                historial.getUsuarioId(),
                historial.getObservacion(),
                historial.getFechaCambio()
        );
    }

    public static List<CrmOportunidadHistorialResponse> toHistorialResponses(List<CrmOportunidadHistorial> historial) {
        return historial.stream().map(CrmMapper::toHistorialResponse).toList();
    }
}
