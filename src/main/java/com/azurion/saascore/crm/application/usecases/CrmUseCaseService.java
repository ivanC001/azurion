package com.azurion.saascore.crm.application.usecases;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.application.dto.CreateClienteRequest;
import com.azurion.saascore.clientes.application.usecases.CreateClienteUseCase;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.CreateCotizacionRequest;
import com.azurion.saascore.cotizaciones.application.usecases.CreateCotizacionUseCase;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.application.dto.CreateCrmActividadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmEtapaPipelineRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmProspectoRequest;
import com.azurion.saascore.crm.application.dto.CrmActividadResponse;
import com.azurion.saascore.crm.application.dto.CrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.CrmDashboardResponse;
import com.azurion.saascore.crm.application.dto.CrmEtapaPipelineResponse;
import com.azurion.saascore.crm.application.dto.CrmEtapaResumenResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadResponse;
import com.azurion.saascore.crm.application.dto.CrmOportunidadHistorialResponse;
import com.azurion.saascore.crm.application.dto.CrmPipelineColumnResponse;
import com.azurion.saascore.crm.application.dto.CrmProspectoResponse;
import com.azurion.saascore.crm.application.dto.CrmReporteBucketResponse;
import com.azurion.saascore.crm.application.dto.CrmReportesResponse;
import com.azurion.saascore.crm.application.dto.GenerarCotizacionDesdeOportunidadRequest;
import com.azurion.saascore.crm.application.dto.MarcarPerdidaRequest;
import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.PublicCrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.RealizarCrmActividadRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmEtapaPipelineRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmOportunidadEtapaRequest;
import com.azurion.saascore.crm.application.dto.UpdateCrmProspectoRequest;
import com.azurion.saascore.crm.application.mappers.CrmMapper;
import com.azurion.saascore.crm.domain.entities.CrmActividad;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmOportunidadHistorial;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmEtapaPipelineRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadHistorialRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrmUseCaseService {

    private static final Set<String> TIPOS_PERSONA = Set.of("NATURAL", "JURIDICA");
    private static final Set<String> ORIGENES = Set.of("WHATSAPP", "FACEBOOK", "INSTAGRAM", "WEB", "REFERIDO", "LLAMADA", "VISITA", "OTRO");
    private static final Set<String> ESTADOS_PROSPECTO = Set.of("NUEVO", "CONTACTADO", "INTERESADO", "NO_INTERESADO", "CONVERTIDO", "DESCARTADO");
    private static final Set<String> ESTADOS_OPORTUNIDAD = Set.of("ABIERTA", "GANADA", "PERDIDA");
    private static final Set<String> TIPOS_COMERCIALES = Set.of(
            "PRODUCTO", "SERVICIO", "VEHICULO", "INMUEBLE", "PROYECTO", "CURSO",
            "SEGURO", "SOFTWARE", "MARKETING", "CLINICA", "JURIDICO", "TURISMO",
            "MAQUINARIA", "FINANCIERO", "EDUCACION", "HOSPITALIDAD", "MANUFACTURA",
            "TELECOMUNICACION", "ENERGIA", "AGRICULTURA", "CONSULTORIA", "OTRO"
    );
    private static final Set<String> ESTADOS_CATALOGO = Set.of("ACTIVO", "INACTIVO", "ARCHIVADO");
    private static final Set<String> TIPOS_ACTIVIDAD = Set.of("LLAMADA", "WHATSAPP", "CORREO", "REUNION", "VISITA", "TAREA", "NOTA");
    private static final Set<String> RESULTADOS_CONTACTO = Set.of("CONTACTADO", "INTERESADO", "REPROGRAMADO", "SIN_RESPUESTA", "NO_INTERESADO", "DESCARTADO", "COTIZACION_SOLICITADA");
    private static final Set<String> NIVELES_INTERES = Set.of("FRIO", "MEDIO", "CALIENTE");
    private static final Set<String> CANALES_INGRESO = Set.of("MANUAL", "LANDING", "WEBHOOK", "WHATSAPP", "FACEBOOK", "IMPORTADO");
    private static final Set<String> MODOS_VALIDACION_PIPELINE = Set.of("STRICT", "WARNING", "FREE");
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final CrmProspectoRepository prospectoRepository;
    private final CrmCatalogoItemRepository catalogoItemRepository;
    private final CrmOportunidadRepository oportunidadRepository;
    private final CrmActividadRepository actividadRepository;
    private final CrmEtapaPipelineRepository etapaPipelineRepository;
    private final CrmOportunidadHistorialRepository historialRepository;
    private final ClienteRepository clienteRepository;
    private final CreateClienteUseCase createClienteUseCase;
    private final CreateCotizacionUseCase createCotizacionUseCase;
    private final CotizacionRepository cotizacionRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public CrmProspectoResponse createProspecto(CreateCrmProspectoRequest request) {
        String responsableId = resolveResponsable(request.responsableId());
        CrmCatalogoItem catalogoItem = request.catalogoItemId() == null ? null : findCatalogoItem(request.catalogoItemId());
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona(requireEnum(request.tipoPersona(), TIPOS_PERSONA, "TIPO_PERSONA_INVALIDO"));
        prospecto.setTipoDocumento(trim(request.tipoDocumento()));
        prospecto.setNumeroDocumento(trim(request.numeroDocumento()));
        prospecto.setNombre(required(request.nombre(), "El nombre del prospecto es obligatorio"));
        prospecto.setRazonSocial(trim(request.razonSocial()));
        prospecto.setNombreComercial(trim(request.nombreComercial()));
        prospecto.setTelefono(trim(request.telefono()));
        prospecto.setCorreo(trim(request.correo()));
        prospecto.setDireccion(trim(request.direccion()));
        prospecto.setOrigen(requireEnum(request.origen(), ORIGENES, "ORIGEN_CRM_INVALIDO"));
        prospecto.setCanalIngreso(defaultEnum(request.canalIngreso(), "MANUAL", CANALES_INGRESO, "CANAL_CRM_INVALIDO"));
        prospecto.setCampania(trim(request.campania()));
        prospecto.setLandingUrl(trim(request.landingUrl()));
        prospecto.setMensaje(trim(request.mensaje()));
        prospecto.setTipoInteres(resolveTipoComercial(firstNonBlank(request.tipoInteres(), catalogoItem == null ? null : catalogoItem.getTipoItem())));
        prospecto.setInteresPrincipal(trim(firstNonBlank(request.interesPrincipal(), catalogoItem == null ? null : catalogoItem.getNombre())));
        prospecto.setInteresDetalle(trim(firstNonBlank(request.interesDetalle(), catalogoItem == null ? null : catalogoItem.getDescripcion())));
        prospecto.setPresupuestoEstimado(request.presupuestoEstimado() == null
                ? catalogoItem == null ? null : catalogoItem.getPrecioReferencial()
                : money(nonNegative(request.presupuestoEstimado())));
        prospecto.setFechaInteres(request.fechaInteres());
        prospecto.setCatalogoItemId(catalogoItem == null ? null : catalogoItem.getId());
        prospecto.setMetadataJson(trim(firstNonBlank(request.metadataJson(), catalogoItem == null ? null : catalogoItem.getMetadataJson())));
        prospecto.setEstado(defaultEnum(request.estado(), "NUEVO", ESTADOS_PROSPECTO, "ESTADO_PROSPECTO_INVALIDO"));
        prospecto.setNivelInteres(resolveInitialInterestLevel(prospecto.getEstado()));
        prospecto.setResponsableId(responsableId);
        prospecto.setObservacion(trim(request.observacion()));
        return CrmMapper.toProspectoResponse(prospectoRepository.save(prospecto));
    }

    @Transactional
    public CrmProspectoResponse capturePublicLead(PublicCrmLeadRequest request) {
        CrmCatalogoItem catalogoItem = findPublicCatalogoItem(request.catalogoItemId(), request.catalogoToken());
        if (hasText(request.website())) {
            throw new BusinessException("CRM_LEAD_PUBLICO_RECHAZADO", "El lead no pudo ser validado");
        }
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona(defaultEnum(request.tipoPersona(), "NATURAL", TIPOS_PERSONA, "TIPO_PERSONA_INVALIDO"));
        prospecto.setTipoDocumento(trim(request.tipoDocumento()));
        prospecto.setNumeroDocumento(trim(request.numeroDocumento()));
        prospecto.setNombre(required(request.nombre(), "El nombre del lead es obligatorio"));
        prospecto.setRazonSocial(trim(request.empresa()));
        prospecto.setNombreComercial(trim(request.empresa()));
        prospecto.setTelefono(trim(request.telefono()));
        prospecto.setCorreo(trim(request.correo()));
        prospecto.setDireccion(trim(request.direccion()));
        prospecto.setOrigen(defaultEnum(request.origen(), "WEB", ORIGENES, "ORIGEN_CRM_INVALIDO"));
        prospecto.setCanalIngreso(defaultEnum(request.canalIngreso(), "LANDING", CANALES_INGRESO, "CANAL_CRM_INVALIDO"));
        prospecto.setCampania(trim(request.campania()));
        prospecto.setLandingUrl(trim(request.landingUrl()));
        prospecto.setMensaje(trim(request.mensaje()));
        prospecto.setTipoInteres(resolveTipoComercial(catalogoItem.getTipoItem()));
        prospecto.setInteresPrincipal(trim(catalogoItem.getNombre()));
        prospecto.setInteresDetalle(trim(firstNonBlank(catalogoItem.getDescripcion(), request.interesDetalle())));
        prospecto.setPresupuestoEstimado(catalogoItem.getPrecioReferencial() == null ? null : money(nonNegative(catalogoItem.getPrecioReferencial())));
        prospecto.setFechaInteres(request.fechaInteres());
        prospecto.setCatalogoItemId(catalogoItem.getId());
        prospecto.setMetadataJson(publicLeadMetadata(request, catalogoItem));
        prospecto.setEstado("NUEVO");
        prospecto.setNivelInteres("FRIO");
        prospecto.setResponsableId("crm-public");
        prospecto.setObservacion(trim(request.mensaje()));
        return CrmMapper.toProspectoResponse(prospectoRepository.save(prospecto));
    }

    @Transactional(readOnly = true)
    public PublicCrmCatalogoItemResponse getPublicCatalogoItem(Long id, String publicToken) {
        CrmCatalogoItem item = findPublicCatalogoItem(id, publicToken);
        return new PublicCrmCatalogoItemResponse(
                item.getId(),
                item.getTipoItem(),
                item.getNombre(),
                item.getDescripcion(),
                item.getPrecioReferencial(),
                item.getMetadataJson()
        );
    }

    @Transactional(readOnly = true)
    public List<CrmCatalogoItemResponse> listCatalogo(String tipoItem) {
        List<CrmCatalogoItem> items = tipoItem == null || tipoItem.isBlank()
                ? catalogoItemRepository.findAllByOrderByIdDesc()
                : catalogoItemRepository.findByTipoItemOrderByIdDesc(resolveTipoComercial(tipoItem));
        return CrmMapper.toCatalogoItemResponses(items);
    }

    @Transactional
    public CrmCatalogoItemResponse createCatalogoItem(CreateCrmCatalogoItemRequest request) {
        CrmCatalogoItem item = new CrmCatalogoItem();
        item.setTipoItem(resolveTipoComercial(request.tipoItem()));
        item.setNombre(required(request.nombre(), "El nombre del item comercial es obligatorio"));
        item.setDescripcion(trim(request.descripcion()));
        item.setPrecioReferencial(money(nonNegative(request.precioReferencial())));
        item.setEstado(defaultEnum(request.estado(), "ACTIVO", ESTADOS_CATALOGO, "ESTADO_CATALOGO_CRM_INVALIDO"));
        item.setMetadataJson(trim(request.metadataJson()));
        item.setPublicEnabled(request.publicEnabled() == null || request.publicEnabled());
        item.setLandingSlug(normalizeSlug(firstNonBlank(request.landingSlug(), request.nombre())));
        item.setPublicToken(generatePublicToken());
        return CrmMapper.toCatalogoItemResponse(catalogoItemRepository.save(item));
    }

    @Transactional
    public CrmCatalogoItemResponse updateCatalogoItem(Long id, UpdateCrmCatalogoItemRequest request) {
        CrmCatalogoItem item = findCatalogoItem(id);
        updateIfPresent(request.tipoItem(), value -> item.setTipoItem(resolveTipoComercial(value)));
        updateIfPresent(request.nombre(), value -> item.setNombre(required(value, "El nombre del item comercial es obligatorio")));
        updateIfPresent(request.descripcion(), value -> item.setDescripcion(trim(value)));
        if (request.precioReferencial() != null) {
            item.setPrecioReferencial(money(nonNegative(request.precioReferencial())));
        }
        updateIfPresent(request.estado(), value -> item.setEstado(requireEnum(value, ESTADOS_CATALOGO, "ESTADO_CATALOGO_CRM_INVALIDO")));
        updateIfPresent(request.metadataJson(), value -> item.setMetadataJson(trim(value)));
        if (request.publicEnabled() != null) {
            item.setPublicEnabled(request.publicEnabled());
        }
        updateIfPresent(request.landingSlug(), value -> item.setLandingSlug(normalizeSlug(value)));
        if (!hasText(item.getPublicToken())) {
            item.setPublicToken(generatePublicToken());
        }
        return CrmMapper.toCatalogoItemResponse(catalogoItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<CrmEtapaPipelineResponse> listEtapas() {
        return CrmMapper.toEtapaResponses(activeStages());
    }

    @Transactional
    public CrmEtapaPipelineResponse createEtapa(CreateCrmEtapaPipelineRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (etapaPipelineRepository.existsByCodigo(codigo)) {
            throw new BusinessException("CRM_ETAPA_DUPLICADA", "Ya existe una etapa con ese codigo");
        }
        CrmEtapaPipeline etapa = new CrmEtapaPipeline();
        etapa.setCodigo(codigo);
        etapa.setNombre(required(request.nombre(), "El nombre de la etapa es obligatorio"));
        etapa.setDescripcion(trim(request.descripcion()));
        etapa.setOrden(request.orden() == null ? nextStageOrder() : request.orden());
        etapa.setProbabilidadDefault(clampProbability(request.probabilidadDefault() == null ? 0 : request.probabilidadDefault()));
        etapa.setColor(firstNonBlank(request.color(), "#2563eb"));
        etapa.setIcono(firstNonBlank(request.icono(), "pi pi-briefcase"));
        etapa.setGanado(Boolean.TRUE.equals(request.ganado()));
        etapa.setPerdido(Boolean.TRUE.equals(request.perdido()));
        etapa.setRequiereValidacion(request.requiereValidacion() == null || request.requiereValidacion());
        etapa.setModoValidacion(defaultEnum(request.modoValidacion(), "WARNING", MODOS_VALIDACION_PIPELINE, "MODO_VALIDACION_PIPELINE_INVALIDO"));
        etapa.setActivo(request.activo() == null || request.activo());
        validateStageFlags(etapa);
        return CrmMapper.toEtapaResponse(etapaPipelineRepository.save(etapa));
    }

    @Transactional
    public CrmEtapaPipelineResponse updateEtapa(Long id, UpdateCrmEtapaPipelineRequest request) {
        CrmEtapaPipeline etapa = findEtapa(id);
        updateIfPresent(request.nombre(), value -> etapa.setNombre(required(value, "El nombre de la etapa es obligatorio")));
        updateIfPresent(request.descripcion(), value -> etapa.setDescripcion(trim(value)));
        if (request.orden() != null) {
            etapa.setOrden(request.orden());
        }
        if (request.probabilidadDefault() != null) {
            etapa.setProbabilidadDefault(clampProbability(request.probabilidadDefault()));
        }
        updateIfPresent(request.color(), value -> etapa.setColor(required(value, "El color de la etapa es obligatorio")));
        updateIfPresent(request.icono(), value -> etapa.setIcono(firstNonBlank(value, "pi pi-briefcase")));
        if (request.ganado() != null) {
            etapa.setGanado(request.ganado());
        }
        if (request.perdido() != null) {
            etapa.setPerdido(request.perdido());
        }
        if (request.requiereValidacion() != null) {
            etapa.setRequiereValidacion(request.requiereValidacion());
        }
        updateIfPresent(request.modoValidacion(), value -> etapa.setModoValidacion(requireEnum(value, MODOS_VALIDACION_PIPELINE, "MODO_VALIDACION_PIPELINE_INVALIDO")));
        if (request.activo() != null) {
            etapa.setActivo(request.activo());
        }
        validateStageFlags(etapa);
        return CrmMapper.toEtapaResponse(etapaPipelineRepository.save(etapa));
    }

    @Transactional(readOnly = true)
    public List<CrmProspectoResponse> listProspectos() {
        return CrmMapper.toProspectoResponses(canViewAll()
                ? prospectoRepository.findAllByOrderByIdDesc()
                : prospectoRepository.findByResponsableIdOrderByIdDesc(currentUserKey()));
    }

    @Transactional(readOnly = true)
    public CrmProspectoResponse getProspecto(Long id) {
        CrmProspecto prospecto = findProspecto(id);
        ensureCanRead(prospecto.getResponsableId());
        return CrmMapper.toProspectoResponse(prospecto);
    }

    @Transactional
    public CrmProspectoResponse updateProspecto(Long id, UpdateCrmProspectoRequest request) {
        CrmProspecto prospecto = findProspecto(id);
        ensureCanWrite(prospecto.getResponsableId());
        updateIfPresent(request.tipoPersona(), value -> prospecto.setTipoPersona(requireEnum(value, TIPOS_PERSONA, "TIPO_PERSONA_INVALIDO")));
        updateIfPresent(request.tipoDocumento(), value -> prospecto.setTipoDocumento(trim(value)));
        updateIfPresent(request.numeroDocumento(), value -> prospecto.setNumeroDocumento(trim(value)));
        updateIfPresent(request.nombre(), value -> prospecto.setNombre(required(value, "El nombre del prospecto es obligatorio")));
        updateIfPresent(request.razonSocial(), value -> prospecto.setRazonSocial(trim(value)));
        updateIfPresent(request.nombreComercial(), value -> prospecto.setNombreComercial(trim(value)));
        updateIfPresent(request.telefono(), value -> prospecto.setTelefono(trim(value)));
        updateIfPresent(request.correo(), value -> prospecto.setCorreo(trim(value)));
        updateIfPresent(request.direccion(), value -> prospecto.setDireccion(trim(value)));
        updateIfPresent(request.origen(), value -> prospecto.setOrigen(requireEnum(value, ORIGENES, "ORIGEN_CRM_INVALIDO")));
        updateIfPresent(request.canalIngreso(), value -> prospecto.setCanalIngreso(requireEnum(value, CANALES_INGRESO, "CANAL_CRM_INVALIDO")));
        updateIfPresent(request.campania(), value -> prospecto.setCampania(trim(value)));
        updateIfPresent(request.landingUrl(), value -> prospecto.setLandingUrl(trim(value)));
        updateIfPresent(request.mensaje(), value -> prospecto.setMensaje(trim(value)));
        updateIfPresent(request.tipoInteres(), value -> prospecto.setTipoInteres(resolveTipoComercial(value)));
        updateIfPresent(request.interesPrincipal(), value -> prospecto.setInteresPrincipal(trim(value)));
        updateIfPresent(request.interesDetalle(), value -> prospecto.setInteresDetalle(trim(value)));
        if (request.presupuestoEstimado() != null) {
            prospecto.setPresupuestoEstimado(money(nonNegative(request.presupuestoEstimado())));
        }
        if (request.fechaInteres() != null) {
            prospecto.setFechaInteres(request.fechaInteres());
        }
        if (request.catalogoItemId() != null) {
            prospecto.setCatalogoItemId(validateCatalogoItemId(request.catalogoItemId()));
        }
        updateIfPresent(request.metadataJson(), value -> prospecto.setMetadataJson(trim(value)));
        updateIfPresent(request.estado(), value -> prospecto.setEstado(requireEnum(value, ESTADOS_PROSPECTO, "ESTADO_PROSPECTO_INVALIDO")));
        updateIfPresent(request.nivelInteres(), value -> prospecto.setNivelInteres(requireEnum(value, NIVELES_INTERES, "NIVEL_INTERES_CRM_INVALIDO")));
        updateIfPresent(request.observacion(), value -> prospecto.setObservacion(trim(value)));
        if (request.responsableId() != null) {
            prospecto.setResponsableId(resolveResponsable(request.responsableId()));
        }
        return CrmMapper.toProspectoResponse(prospectoRepository.save(prospecto));
    }

    @Transactional
    public ClienteResponse convertirProspectoCliente(Long id) {
        CrmProspecto prospecto = findProspecto(id);
        ensureCanWrite(prospecto.getResponsableId());
        if (prospecto.getClienteId() != null || "CONVERTIDO".equals(prospecto.getEstado())) {
            throw new BusinessException("PROSPECTO_YA_CONVERTIDO", "El prospecto ya fue convertido a cliente");
        }
        String tipoDocumento = required(prospecto.getTipoDocumento(), "El prospecto necesita tipo de documento para convertirse en cliente");
        String numeroDocumento = required(prospecto.getNumeroDocumento(), "El prospecto necesita numero de documento para convertirse en cliente");
        String nombre = "JURIDICA".equals(prospecto.getTipoPersona())
                ? firstNonBlank(prospecto.getRazonSocial(), prospecto.getNombreComercial(), prospecto.getNombre())
                : prospecto.getNombre();

        ClienteResponse cliente = createClienteUseCase.execute(new CreateClienteRequest(
                tipoDocumento,
                numeroDocumento,
                nombre,
                prospecto.getCorreo(),
                prospecto.getDireccion(),
                null,
                prospecto.getTelefono(),
                BigDecimal.ZERO,
                0,
                true
        ));
        prospecto.setClienteId(cliente.id());
        prospecto.setEstado("CONVERTIDO");
        prospecto.setFechaConversion(OffsetDateTime.now());
        prospectoRepository.save(prospecto);
        return cliente;
    }

    @Transactional
    public CrmOportunidadResponse createOportunidad(CreateCrmOportunidadRequest request) {
        String responsableId = resolveResponsable(request.responsableId());
        CrmOportunidad oportunidad = new CrmOportunidad();
        applyOportunidadLinks(oportunidad, request.prospectoId(), request.clienteId());
        String tipoBase = oportunidad.getProspecto() == null ? null : oportunidad.getProspecto().getTipoInteres();
        oportunidad.setTipoOportunidad(resolveTipoOportunidad(firstNonBlank(request.tipoOportunidad(), tipoBase)));
        oportunidad.setCatalogoItemId(resolveCatalogoItemForOpportunity(request.catalogoItemId(), oportunidad.getProspecto()));
        oportunidad.setTitulo(required(request.titulo(), "El titulo de la oportunidad es obligatorio"));
        oportunidad.setDescripcion(trim(request.descripcion()));
        oportunidad.setMontoEstimado(money(nonNegative(request.montoEstimado())));
        oportunidad.setProbabilidad(clampProbability(request.probabilidad()));
        applyStage(oportunidad, resolveStageByCode(firstNonBlank(request.etapa(), "NUEVO")), "Creacion de oportunidad", false);
        oportunidad.setFechaCierreEstimada(request.fechaCierreEstimada());
        oportunidad.setResponsableId(responsableId);
        oportunidad.setEstado("ABIERTA");
        ensureNoActiveOpportunityDuplicate(oportunidad);
        return CrmMapper.toOportunidadResponse(oportunidadRepository.save(oportunidad));
    }

    @Transactional(readOnly = true)
    public List<CrmOportunidadResponse> listOportunidades() {
        return CrmMapper.toOportunidadResponses(canViewAll()
                ? oportunidadRepository.findAllByOrderByIdDesc()
                : oportunidadRepository.findByResponsableIdOrderByIdDesc(currentUserKey()));
    }

    @Transactional(readOnly = true)
    public List<CrmPipelineColumnResponse> pipeline() {
        List<CrmOportunidad> oportunidades = scopedOportunidades();
        return activeStages().stream()
                .map(etapa -> {
                    List<CrmOportunidad> matches = oportunidades.stream()
                            .filter(oportunidad -> oportunidad.getEtapaPipeline() != null
                                    && etapa.getId().equals(oportunidad.getEtapaPipeline().getId()))
                            .sorted(Comparator.comparing(CrmOportunidad::getFechaUltimaActualizacion,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();
                    BigDecimal monto = sumAmount(matches);
                    return new CrmPipelineColumnResponse(
                            CrmMapper.toEtapaResponse(etapa),
                            matches.size(),
                            monto,
                            CrmMapper.toOportunidadResponses(matches)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CrmOportunidadResponse getOportunidad(Long id) {
        CrmOportunidad oportunidad = findOportunidad(id);
        ensureCanRead(oportunidad.getResponsableId());
        return CrmMapper.toOportunidadResponse(oportunidad);
    }

    @Transactional(readOnly = true)
    public List<CrmOportunidadHistorialResponse> historialOportunidad(Long id) {
        CrmOportunidad oportunidad = findOportunidad(id);
        ensureCanRead(oportunidad.getResponsableId());
        return CrmMapper.toHistorialResponses(historialRepository.findByOportunidadIdOrderByFechaCambioDescIdDesc(id));
    }

    @Transactional
    public CrmOportunidadResponse updateOportunidad(Long id, UpdateCrmOportunidadRequest request) {
        CrmOportunidad oportunidad = findOportunidad(id);
        ensureCanWrite(oportunidad.getResponsableId());
        if (request.prospectoId() != null || request.clienteId() != null) {
            applyOportunidadLinks(oportunidad, request.prospectoId(), request.clienteId());
        }
        updateIfPresent(request.tipoOportunidad(), value -> oportunidad.setTipoOportunidad(resolveTipoOportunidad(value)));
        if (request.catalogoItemId() != null) {
            oportunidad.setCatalogoItemId(validateCatalogoItemId(request.catalogoItemId()));
        }
        updateIfPresent(request.titulo(), value -> oportunidad.setTitulo(required(value, "El titulo de la oportunidad es obligatorio")));
        updateIfPresent(request.descripcion(), value -> oportunidad.setDescripcion(trim(value)));
        if (request.montoEstimado() != null) {
            oportunidad.setMontoEstimado(money(nonNegative(request.montoEstimado())));
        }
        if (request.probabilidad() != null) {
            oportunidad.setProbabilidad(clampProbability(request.probabilidad()));
        }
        updateIfPresent(request.etapa(), value -> applyStage(oportunidad, resolveStageByCode(value), "Actualizacion manual de etapa", true));
        if (request.fechaCierreEstimada() != null) {
            oportunidad.setFechaCierreEstimada(request.fechaCierreEstimada());
        }
        updateIfPresent(request.estado(), value -> oportunidad.setEstado(requireEnum(value, ESTADOS_OPORTUNIDAD, "ESTADO_OPORTUNIDAD_INVALIDO")));
        updateIfPresent(request.motivoPerdida(), value -> oportunidad.setMotivoPerdida(trim(value)));
        if (request.responsableId() != null) {
            oportunidad.setResponsableId(resolveResponsable(request.responsableId()));
        }
        ensureNoActiveOpportunityDuplicate(oportunidad);
        return CrmMapper.toOportunidadResponse(oportunidadRepository.save(oportunidad));
    }

    @Transactional
    public CrmOportunidadResponse moverOportunidadEtapa(Long id, UpdateCrmOportunidadEtapaRequest request) {
        CrmOportunidad oportunidad = findOportunidad(id);
        ensureCanWrite(oportunidad.getResponsableId());
        CrmEtapaPipeline destino = findEtapaActiva(request.etapaId());
        if (destino.isPerdido() && !hasText(request.observacion())) {
            throw new BusinessException("CRM_MOTIVO_PERDIDA_REQUERIDO", "Indica el motivo de perdida antes de cerrar la oportunidad");
        }
        applyStage(oportunidad, destino, request.observacion(), true);
        return CrmMapper.toOportunidadResponse(oportunidadRepository.save(oportunidad));
    }

    @Transactional
    public CrmOportunidadResponse marcarGanada(Long id) {
        CrmOportunidad oportunidad = findOportunidad(id);
        ensureCanWrite(oportunidad.getResponsableId());
        applyStage(oportunidad, resolveStageByCode("GANADO"), "Oportunidad ganada", true);
        oportunidad.setProbabilidad(100);
        oportunidad.setMotivoPerdida(null);
        oportunidad.setMontoReal(oportunidad.getMontoEstimado());
        return CrmMapper.toOportunidadResponse(oportunidadRepository.save(oportunidad));
    }

    @Transactional
    public CrmOportunidadResponse marcarPerdida(Long id, MarcarPerdidaRequest request) {
        CrmOportunidad oportunidad = findOportunidad(id);
        ensureCanWrite(oportunidad.getResponsableId());
        String motivo = required(request.motivo(), "Indica el motivo de perdida");
        applyStage(oportunidad, resolveStageByCode("PERDIDO"), motivo, true);
        oportunidad.setProbabilidad(0);
        oportunidad.setMotivoPerdida(motivo);
        return CrmMapper.toOportunidadResponse(oportunidadRepository.save(oportunidad));
    }

    @Transactional
    public CotizacionResponse generarCotizacion(Long oportunidadId, GenerarCotizacionDesdeOportunidadRequest request) {
        CrmOportunidad oportunidad = findOportunidad(oportunidadId);
        ensureCanWrite(oportunidad.getResponsableId());
        if ("PERDIDA".equals(oportunidad.getEstado())) {
            throw new BusinessException("OPORTUNIDAD_PERDIDA", "No se puede cotizar una oportunidad perdida");
        }
        Long clienteId = request.clienteId() != null ? request.clienteId()
                : oportunidad.getCliente() == null ? null : oportunidad.getCliente().getId();
        CotizacionResponse cotizacion = createCotizacionUseCase.execute(new CreateCotizacionRequest(
                clienteId,
                request.usuarioId(),
                request.usuarioNombre(),
                request.sucursalId(),
                request.fechaEmision(),
                request.fechaVencimiento(),
                request.moneda(),
                request.observacion(),
                oportunidad.getId(),
                request.detalles()
        ));
        applyStage(oportunidad, resolveStageByCode("COTIZADO"), "Cotizacion generada desde CRM", true);
        oportunidadRepository.save(oportunidad);
        return cotizacion;
    }

    @Transactional
    public CrmActividadResponse createActividad(CreateCrmActividadRequest request) {
        String usuarioId = resolveResponsibleForActivity(request.usuarioId());
        CrmActividad actividad = new CrmActividad();
        applyActividadLinks(actividad, request.prospectoId(), request.oportunidadId(), request.clienteId());
        actividad.setTipoActividad(requireEnum(request.tipoActividad(), TIPOS_ACTIVIDAD, "TIPO_ACTIVIDAD_CRM_INVALIDO"));
        actividad.setAsunto(required(request.asunto(), "El asunto de la actividad es obligatorio"));
        actividad.setDescripcion(trim(request.descripcion()));
        actividad.setFechaProgramada(request.fechaProgramada());
        actividad.setUsuarioId(usuarioId);
        actividad.setEstado("PENDIENTE");
        return CrmMapper.toActividadResponse(actividadRepository.save(actividad));
    }

    @Transactional(readOnly = true)
    public List<CrmActividadResponse> listActividades() {
        return CrmMapper.toActividadResponses(canViewAll()
                ? actividadRepository.findAllByOrderByFechaProgramadaAscIdDesc()
                : actividadRepository.findByUsuarioIdOrderByFechaProgramadaAscIdDesc(currentUserKey()));
    }

    @Transactional(readOnly = true)
    public CrmActividadResponse getActividad(Long id) {
        CrmActividad actividad = findActividad(id);
        ensureCanRead(actividad.getUsuarioId());
        return CrmMapper.toActividadResponse(actividad);
    }

    @Transactional
    public CrmActividadResponse realizarActividad(Long id, RealizarCrmActividadRequest request) {
        CrmActividad actividad = findActividad(id);
        ensureCanWrite(actividad.getUsuarioId());
        String resultadoContacto = optionalEnum(request.resultadoContacto(), RESULTADOS_CONTACTO, "RESULTADO_CONTACTO_CRM_INVALIDO");
        String nivelInteres = optionalEnum(request.nivelInteres(), NIVELES_INTERES, "NIVEL_INTERES_CRM_INVALIDO");
        String estadoProspecto = optionalEnum(request.estadoProspecto(), ESTADOS_PROSPECTO, "ESTADO_PROSPECTO_INVALIDO");
        actividad.setEstado("REALIZADA");
        actividad.setFechaRealizada(OffsetDateTime.now());
        actividad.setResultado(trim(request.resultado()));
        actividad.setResultadoContacto(resultadoContacto);
        actividad.setNivelInteres(nivelInteres);
        actividad.setEstadoProspectoResultado(estadoProspecto);
        applyActivityResultToProspect(actividad, resultadoContacto, nivelInteres, estadoProspecto);
        return CrmMapper.toActividadResponse(actividadRepository.save(actividad));
    }

    @Transactional
    public CrmActividadResponse cancelarActividad(Long id, RealizarCrmActividadRequest request) {
        CrmActividad actividad = findActividad(id);
        ensureCanWrite(actividad.getUsuarioId());
        actividad.setEstado("CANCELADA");
        actividad.setFechaRealizada(OffsetDateTime.now());
        actividad.setResultado(trim(request.resultado()));
        actividad.setResultadoContacto(optionalEnum(request.resultadoContacto(), RESULTADOS_CONTACTO, "RESULTADO_CONTACTO_CRM_INVALIDO"));
        actividad.setNivelInteres(optionalEnum(request.nivelInteres(), NIVELES_INTERES, "NIVEL_INTERES_CRM_INVALIDO"));
        actividad.setEstadoProspectoResultado(optionalEnum(request.estadoProspecto(), ESTADOS_PROSPECTO, "ESTADO_PROSPECTO_INVALIDO"));
        return CrmMapper.toActividadResponse(actividadRepository.save(actividad));
    }

    @Transactional(readOnly = true)
    public CrmDashboardResponse dashboard() {
        List<CrmOportunidad> oportunidades = scopedOportunidades();
        boolean viewAll = canViewAll();
        String current = currentUserKey();
        return new CrmDashboardResponse(
                viewAll ? prospectoRepository.countByEstado("NUEVO") : prospectoRepository.countByResponsableIdAndEstado(current, "NUEVO"),
                viewAll ? prospectoRepository.countByEstado("CONVERTIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "CONVERTIDO"),
                viewAll ? oportunidadRepository.countByEstado("ABIERTA") : oportunidadRepository.countByResponsableIdAndEstado(current, "ABIERTA"),
                viewAll ? oportunidadRepository.countByEstado("GANADA") : oportunidadRepository.countByResponsableIdAndEstado(current, "GANADA"),
                viewAll ? oportunidadRepository.countByEstado("PERDIDA") : oportunidadRepository.countByResponsableIdAndEstado(current, "PERDIDA"),
                viewAll ? actividadRepository.countByEstado("PENDIENTE") : actividadRepository.countByUsuarioIdAndEstado(current, "PENDIENTE"),
                viewAll
                        ? actividadRepository.countByEstadoAndFechaProgramadaBefore("PENDIENTE", OffsetDateTime.now())
                        : actividadRepository.countByUsuarioIdAndEstadoAndFechaProgramadaBefore(current, "PENDIENTE", OffsetDateTime.now()),
                viewAll ? prospectoRepository.countByCanalIngresoNot("MANUAL") : 0,
                viewAll ? prospectoRepository.countByCanalIngreso("MANUAL") : 0,
                sumPipeline(oportunidades),
                resumenPorEtapa(oportunidades)
        );
    }

    @Transactional(readOnly = true)
    public CrmReportesResponse reportes() {
        boolean viewAll = canViewAll();
        String current = currentUserKey();
        return new CrmReportesResponse(
                resumenPorEtapa(scopedOportunidades()),
                viewAll ? actividadRepository.countByEstado("PENDIENTE") : actividadRepository.countByUsuarioIdAndEstado(current, "PENDIENTE"),
                viewAll ? actividadRepository.countByEstado("REALIZADA") : actividadRepository.countByUsuarioIdAndEstado(current, "REALIZADA"),
                viewAll ? prospectoRepository.countByEstado("CONVERTIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "CONVERTIDO"),
                viewAll ? prospectoRepository.countByEstado("DESCARTADO") : prospectoRepository.countByResponsableIdAndEstado(current, "DESCARTADO")
        );
    }

    @Transactional(readOnly = true)
    public List<CrmReporteBucketResponse> reporteOportunidadesEtapa() {
        return resumenPorEtapa(scopedOportunidades()).stream()
                .map(item -> new CrmReporteBucketResponse(item.etapa(), item.etapa(), item.cantidad(), item.monto()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrmReporteBucketResponse> reporteOportunidadesVendedor() {
        return scopedOportunidades().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CrmOportunidad::getResponsableId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> new CrmReporteBucketResponse(
                        entry.getKey(),
                        entry.getKey(),
                        entry.getValue().size(),
                        sumAmount(entry.getValue())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reporteConversiones() {
        boolean viewAll = canViewAll();
        String current = currentUserKey();
        long prospectos = viewAll ? prospectoRepository.count() : prospectoRepository.findByResponsableIdOrderByIdDesc(current).size();
        long convertidos = viewAll ? prospectoRepository.countByEstado("CONVERTIDO") : prospectoRepository.countByResponsableIdAndEstado(current, "CONVERTIDO");
        long oportunidades = scopedOportunidades().size();
        long ganadas = scopedOportunidades().stream().filter(item -> "GANADA".equals(item.getEstado())).count();
        long cotizadas = scopedOportunidades().stream().filter(item -> "COTIZADO".equals(item.getEtapa()) || "GANADA".equals(item.getEstado())).count();
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
        List<CrmProspecto> prospectos = canViewAll()
                ? prospectoRepository.findAllByOrderByIdDesc()
                : prospectoRepository.findByResponsableIdOrderByIdDesc(currentUserKey());
        return prospectos.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CrmProspecto::getOrigen,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new CrmReporteBucketResponse(entry.getKey(), entry.getKey(), entry.getValue(), BigDecimal.ZERO))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reporteGanadasPerdidas() {
        List<CrmOportunidad> oportunidades = scopedOportunidades();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ganadas", oportunidades.stream().filter(item -> "GANADA".equals(item.getEstado())).count());
        response.put("perdidas", oportunidades.stream().filter(item -> "PERDIDA".equals(item.getEstado())).count());
        response.put("montoGanado", sumAmount(oportunidades.stream().filter(item -> "GANADA".equals(item.getEstado())).toList()));
        response.put("montoPerdido", sumAmount(oportunidades.stream().filter(item -> "PERDIDA".equals(item.getEstado())).toList()));
        return response;
    }

    private void applyStage(CrmOportunidad oportunidad, CrmEtapaPipeline destino, String observacion, boolean registrarHistorial) {
        CrmEtapaPipeline origen = oportunidad.getEtapaPipeline();
        if (origen != null && origen.getId().equals(destino.getId())) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        oportunidad.setEtapaPipeline(destino);
        oportunidad.setEtapa(destino.getCodigo());
        oportunidad.setFechaUltimaActualizacion(now);
        if (destino.isGanado()) {
            oportunidad.setEstado("GANADA");
            oportunidad.setProbabilidad(100);
            oportunidad.setFechaGanada(now);
            oportunidad.setFechaCierreReal(now);
            oportunidad.setMotivoPerdida(null);
            oportunidad.setMontoReal(oportunidad.getMontoEstimado());
        } else if (destino.isPerdido()) {
            oportunidad.setEstado("PERDIDA");
            oportunidad.setProbabilidad(0);
            oportunidad.setFechaPerdida(now);
            oportunidad.setFechaCierreReal(now);
        } else {
            oportunidad.setEstado("ABIERTA");
            oportunidad.setFechaCierreReal(null);
        }
        if (registrarHistorial) {
            CrmOportunidadHistorial historial = new CrmOportunidadHistorial();
            historial.setOportunidad(oportunidad);
            historial.setEtapaOrigen(origen);
            historial.setEtapaDestino(destino);
            historial.setUsuarioId(currentUserKey());
            historial.setObservacion(trim(observacion));
            historial.setFechaCambio(now);
            historialRepository.save(historial);
        }
    }

    private List<CrmEtapaPipeline> activeStages() {
        return etapaPipelineRepository.findByActivoTrueOrderByOrdenAscIdAsc();
    }

    private CrmEtapaPipeline resolveStageByCode(String codigo) {
        return etapaPipelineRepository.findByCodigo(normalizeCode(codigo))
                .filter(CrmEtapaPipeline::isActivo)
                .orElseThrow(() -> new BusinessException("ETAPA_CRM_INVALIDA", "Etapa CRM no encontrada o inactiva"));
    }

    private CrmEtapaPipeline findEtapa(Long id) {
        return etapaPipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ETAPA_CRM_NO_ENCONTRADA", "Etapa CRM no encontrada"));
    }

    private CrmEtapaPipeline findEtapaActiva(Long id) {
        return etapaPipelineRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new BusinessException("ETAPA_CRM_NO_ENCONTRADA", "Etapa CRM no encontrada o inactiva"));
    }

    private int nextStageOrder() {
        return activeStages().stream().map(CrmEtapaPipeline::getOrden).max(Integer::compareTo).orElse(0) + 1;
    }

    private void validateStageFlags(CrmEtapaPipeline etapa) {
        if (etapa.isGanado() && etapa.isPerdido()) {
            throw new BusinessException("CRM_ETAPA_INVALIDA", "Una etapa no puede ser ganada y perdida a la vez");
        }
    }

    private BigDecimal sumAmount(List<CrmOportunidad> oportunidades) {
        return oportunidades.stream()
                .map(oportunidad -> oportunidad.getMontoReal() != null ? oportunidad.getMontoReal() : oportunidad.getMontoEstimado())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal conversion(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private void applyOportunidadLinks(CrmOportunidad oportunidad, Long prospectoId, Long clienteId) {
        if (prospectoId == null && clienteId == null && oportunidad.getProspecto() == null && oportunidad.getCliente() == null) {
            throw new BusinessException("OPORTUNIDAD_SIN_RELACION", "La oportunidad necesita prospecto o cliente");
        }
        if (prospectoId != null) {
            CrmProspecto prospecto = findProspecto(prospectoId);
            ensureCanRead(prospecto.getResponsableId());
            oportunidad.setProspecto(prospecto);
            if (clienteId == null && prospecto.getClienteId() != null) {
                oportunidad.setCliente(findCliente(prospecto.getClienteId()));
            }
        }
        if (clienteId != null) {
            oportunidad.setCliente(findCliente(clienteId));
        }
    }

    private void ensureNoActiveOpportunityDuplicate(CrmOportunidad oportunidad) {
        if (!"ABIERTA".equals(oportunidad.getEstado()) || oportunidad.getProspecto() == null) {
            return;
        }
        oportunidadRepository
                .findFirstByProspectoIdAndEstadoOrderByIdDesc(oportunidad.getProspecto().getId(), "ABIERTA")
                .filter(existing -> oportunidad.getId() == null || !existing.getId().equals(oportunidad.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "CRM_OPORTUNIDAD_ACTIVA_EXISTE",
                            "Este prospecto ya tiene una oportunidad activa. Usa la oportunidad existente."
                    );
                });
    }

    private void applyActividadLinks(CrmActividad actividad, Long prospectoId, Long oportunidadId, Long clienteId) {
        if (prospectoId == null && oportunidadId == null && clienteId == null) {
            throw new BusinessException("ACTIVIDAD_SIN_RELACION", "La actividad necesita prospecto, oportunidad o cliente");
        }
        if (prospectoId != null) {
            CrmProspecto prospecto = findProspecto(prospectoId);
            ensureCanRead(prospecto.getResponsableId());
            actividad.setProspecto(prospecto);
        }
        if (oportunidadId != null) {
            CrmOportunidad oportunidad = findOportunidad(oportunidadId);
            ensureCanRead(oportunidad.getResponsableId());
            actividad.setOportunidad(oportunidad);
        }
        if (clienteId != null) {
            actividad.setCliente(findCliente(clienteId));
        }
    }

    private List<CrmOportunidad> scopedOportunidades() {
        return canViewAll()
                ? oportunidadRepository.findAllByOrderByIdDesc()
                : oportunidadRepository.findByResponsableIdOrderByIdDesc(currentUserKey());
    }

    private BigDecimal sumPipeline(List<CrmOportunidad> oportunidades) {
        return oportunidades.stream()
                .filter(oportunidad -> "ABIERTA".equals(oportunidad.getEstado()))
                .map(CrmOportunidad::getMontoEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<CrmEtapaResumenResponse> resumenPorEtapa(List<CrmOportunidad> oportunidades) {
        return activeStages().stream()
                .map(etapa -> {
                    List<CrmOportunidad> matches = oportunidades.stream()
                            .filter(oportunidad -> oportunidad.getEtapaPipeline() != null
                                    && etapa.getId().equals(oportunidad.getEtapaPipeline().getId()))
                            .toList();
                    BigDecimal monto = matches.stream()
                            .map(CrmOportunidad::getMontoEstimado)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new CrmEtapaResumenResponse(etapa.getCodigo(), matches.size(), monto);
                })
                .toList();
    }

    private CrmProspecto findProspecto(Long id) {
        return prospectoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PROSPECTO_NO_ENCONTRADO", "Prospecto CRM no encontrado"));
    }

    private CrmOportunidad findOportunidad(Long id) {
        return oportunidadRepository.findById(id)
                .orElseThrow(() -> new BusinessException("OPORTUNIDAD_NO_ENCONTRADA", "Oportunidad CRM no encontrada"));
    }

    private CrmActividad findActividad(Long id) {
        return actividadRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ACTIVIDAD_NO_ENCONTRADA", "Actividad CRM no encontrada"));
    }

    private Cliente findCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CLIENTE_NO_ENCONTRADO", "Cliente no encontrado"));
    }

    private CrmCatalogoItem findCatalogoItem(Long id) {
        return catalogoItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CRM_CATALOGO_NO_ENCONTRADO", "Item de catalogo CRM no encontrado"));
    }

    private CrmCatalogoItem findPublicCatalogoItem(Long id, String publicToken) {
        if (id == null || !hasText(publicToken)) {
            throw new BusinessException("CRM_CATALOGO_PUBLICO_REQUERIDO", "La landing debe enviar una oferta CRM valida");
        }
        CrmCatalogoItem item = catalogoItemRepository.findByIdAndPublicToken(id, publicToken.trim())
                .orElseThrow(() -> new BusinessException("CRM_CATALOGO_PUBLICO_INVALIDO", "La oferta CRM no es valida para esta landing"));
        if (!item.isPublicEnabled() || !"ACTIVO".equals(item.getEstado())) {
            throw new BusinessException("CRM_CATALOGO_PUBLICO_INACTIVO", "La oferta CRM no esta disponible para captar leads");
        }
        return item;
    }

    private String resolveResponsable(String requested) {
        String current = currentUserKey();
        String resolved = firstNonBlank(requested, current);
        if (!resolved.equals(current) && !hasAuthority("CRM_ASSIGN") && !canViewAll()) {
            throw new BusinessException("CRM_ASIGNACION_NO_PERMITIDA", "No puedes asignar registros CRM a otro responsable");
        }
        return resolved;
    }

    private String resolveResponsibleForActivity(String requested) {
        return resolveResponsable(requested);
    }

    private void ensureCanRead(String owner) {
        if (!canViewAll() && !currentUserKey().equals(owner)) {
            throw new BusinessException("CRM_SIN_ACCESO", "No tienes acceso a este registro CRM");
        }
    }

    private void ensureCanWrite(String owner) {
        ensureCanRead(owner);
    }

    private boolean canViewAll() {
        return hasAuthority("CRM_VIEW_ALL")
                || hasAuthority("ROLE_ADMIN_GENERAL")
                || hasAuthority("ROLE_PLATFORM_ADMIN")
                || hasAuthority("ROLE_ADMIN_EMPRESA")
                || hasAuthority("ROLE_ADMIN");
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private String currentUserKey() {
        Long usuarioId = authorizationService.currentUsuarioId();
        if (usuarioId != null) {
            return String.valueOf(usuarioId);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "system";
    }

    private String requireEnum(String value, Set<String> allowed, String code) {
        String normalized = normalize(value);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(code, "Valor CRM invalido: " + value);
        }
        return normalized;
    }

    private String optionalEnum(String value, Set<String> allowed, String code) {
        String trimmed = trim(value);
        return trimmed == null ? null : requireEnum(trimmed, allowed, code);
    }

    private String defaultEnum(String value, String defaultValue, Set<String> allowed, String code) {
        return value == null || value.isBlank() ? defaultValue : requireEnum(value, allowed, code);
    }

    private void applyActivityResultToProspect(CrmActividad actividad, String resultadoContacto, String nivelInteres, String estadoProspecto) {
        CrmProspecto prospecto = actividad.getProspecto();
        if (prospecto == null && actividad.getOportunidad() != null) {
            prospecto = actividad.getOportunidad().getProspecto();
        }
        if (prospecto == null) {
            return;
        }
        ensureCanWrite(prospecto.getResponsableId());
        String resolvedEstado = firstNonBlank(estadoProspecto, prospectStatusByResult(resultadoContacto));
        String resolvedNivel = firstNonBlank(nivelInteres, interestLevelByResult(resultadoContacto), resolveInitialInterestLevel(resolvedEstado));
        if (hasText(resolvedEstado)) {
            prospecto.setEstado(requireEnum(resolvedEstado, ESTADOS_PROSPECTO, "ESTADO_PROSPECTO_INVALIDO"));
            actividad.setEstadoProspectoResultado(prospecto.getEstado());
        }
        if (hasText(resolvedNivel)) {
            prospecto.setNivelInteres(requireEnum(resolvedNivel, NIVELES_INTERES, "NIVEL_INTERES_CRM_INVALIDO"));
            actividad.setNivelInteres(prospecto.getNivelInteres());
        }
        if (hasText(actividad.getResultado())) {
            prospecto.setObservacion(actividad.getResultado());
        }
        prospectoRepository.save(prospecto);
    }

    private String prospectStatusByResult(String resultadoContacto) {
        return switch (resultadoContacto == null ? "" : resultadoContacto) {
            case "CONTACTADO", "REPROGRAMADO" -> "CONTACTADO";
            case "INTERESADO", "COTIZACION_SOLICITADA" -> "INTERESADO";
            case "NO_INTERESADO" -> "NO_INTERESADO";
            case "DESCARTADO" -> "DESCARTADO";
            default -> null;
        };
    }

    private String interestLevelByResult(String resultadoContacto) {
        return switch (resultadoContacto == null ? "" : resultadoContacto) {
            case "INTERESADO", "COTIZACION_SOLICITADA" -> "CALIENTE";
            case "CONTACTADO", "REPROGRAMADO" -> "MEDIO";
            case "SIN_RESPUESTA", "NO_INTERESADO", "DESCARTADO" -> "FRIO";
            default -> null;
        };
    }

    private String resolveInitialInterestLevel(String estado) {
        return switch (firstNonBlank(estado, "NUEVO")) {
            case "INTERESADO" -> "CALIENTE";
            case "CONTACTADO" -> "MEDIO";
            default -> "FRIO";
        };
    }

    private String resolveTipoOportunidad(String value) {
        return defaultEnum(value, "PRODUCTO", TIPOS_COMERCIALES, "TIPO_OPORTUNIDAD_INVALIDO");
    }

    private String resolveTipoComercial(String value) {
        return defaultEnum(value, "PRODUCTO", TIPOS_COMERCIALES, "TIPO_COMERCIAL_CRM_INVALIDO");
    }

    private Long validateCatalogoItemId(Long id) {
        if (id == null) {
            return null;
        }
        findCatalogoItem(id);
        return id;
    }

    private Long resolveCatalogoItemForOpportunity(Long requestId, CrmProspecto prospecto) {
        if (requestId != null) {
            return validateCatalogoItemId(requestId);
        }
        return prospecto == null ? null : prospecto.getCatalogoItemId();
    }

    private String required(String value, String message) {
        String normalized = trim(value);
        if (normalized == null) {
            throw new BusinessException("CRM_DATO_REQUERIDO", message);
        }
        return normalized;
    }

    private String normalize(String value) {
        return required(value, "Campo CRM obligatorio").toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return normalize(value).replaceAll("[^A-Z0-9_]", "_");
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("CRM_MONTO_INVALIDO", "El monto no puede ser negativo");
        }
        return resolved;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String generatePublicToken() {
        String token;
        do {
            byte[] bytes = new byte[24];
            TOKEN_RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (catalogoItemRepository.existsByPublicToken(token));
        return token;
    }

    private String normalizeSlug(String value) {
        String base = firstNonBlank(value, "oferta-crm");
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "oferta-crm" : normalized.substring(0, Math.min(140, normalized.length()));
    }

    private String publicLeadMetadata(PublicCrmLeadRequest request, CrmCatalogoItem catalogoItem) {
        return """
                {"source":"public-crm-lead","catalogoItemId":%d,"catalogoTokenValidado":true,"tipoItem":"%s","oferta":"%s","precioReferencial":"%s","landingUrl":"%s","campania":"%s","payloadCliente":"%s","catalogoMetadata":"%s"}
                """.formatted(
                catalogoItem.getId(),
                json(catalogoItem.getTipoItem()),
                json(catalogoItem.getNombre()),
                catalogoItem.getPrecioReferencial() == null ? "0.00" : catalogoItem.getPrecioReferencial().toPlainString(),
                json(request.landingUrl()),
                json(request.campania()),
                json(request.metadataJson()),
                json(catalogoItem.getMetadataJson())
        ).trim();
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private Integer clampProbability(Integer value) {
        int resolved = value == null ? 0 : value;
        if (resolved < 0 || resolved > 100) {
            throw new BusinessException("CRM_PROBABILIDAD_INVALIDA", "La probabilidad debe estar entre 0 y 100");
        }
        return resolved;
    }

    private <T> void updateIfPresent(T value, java.util.function.Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
