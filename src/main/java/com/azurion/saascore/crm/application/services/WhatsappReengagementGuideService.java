package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CrmWhatsappTemplateResponse;
import com.azurion.saascore.crm.application.dto.WhatsappReengagementGuideResponse;
import com.azurion.saascore.crm.application.dto.WhatsappReengagementGuideResponse.PlantillaSugerida;
import com.azurion.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explica al usuario que le falta para poder reenganchar por WhatsApp.
 *
 * <p>El motivo de que exista: fuera de la ventana de 24 horas Meta solo acepta
 * plantillas aprobadas, y las reglas de que plantilla sirve no son obvias. Sin esta
 * guia el usuario descubre los problemas de a uno, y siempre tarde: la plantilla de
 * ejemplo que solo corre en numeros de prueba, el encabezado de imagen que el
 * compositor no admite, la categoria que encarece cada envio.
 */
@Service
@RequiredArgsConstructor
public class WhatsappReengagementGuideService {

    /** Plantillas de muestra de Meta: solo salen desde sus numeros de prueba. */
    private static final Set<String> PLANTILLAS_DE_EJEMPLO = Set.of("hello_world", "sample_issue_resolution");

    private static final PlantillaSugerida MODELO = new PlantillaSugerida(
            "UTILITY",
            "Utility entrega mejor y cuesta menos que Marketing, pero solo califica si el "
                    + "mensaje habla de algo concreto que el cliente pidio. Por eso el modelo cita "
                    + "una cotizacion real con su numero, monto y vencimiento. Un texto del tipo "
                    + "\"¿sigues interesado?\" es reenganche generico y Meta lo va a clasificar "
                    + "como Marketing, que tambien funciona pero se factura mas caro y queda "
                    + "sujeto a los limites de entrega de Meta.",
            """
            Hola {{1}}, tu cotizacion #{{2}} por {{3}} quedo por un total de {{4}} y vence el {{5}}.

            Responde a este mensaje si deseas confirmarla o necesitas ajustar algo.""",
            List.of("Deseo continuar", "Ahora no"),
            List.of(
                    "{{1}} nombre del prospecto",
                    "{{2}} numero de la cotizacion",
                    "{{3}} producto o servicio cotizado",
                    "{{4}} total con su moneda",
                    "{{5}} fecha de vencimiento"
            )
    );

    private final WhatsappIntegrationService whatsappIntegrationService;

    @Transactional(readOnly = true)
    public WhatsappReengagementGuideResponse guide() {
        List<CrmWhatsappTemplateResponse> plantillas;
        String problemaDeConfiguracion = null;
        try {
            plantillas = whatsappIntegrationService.listApprovedTemplates();
        } catch (BusinessException error) {
            plantillas = List.of();
            problemaDeConfiguracion = motivoDeConfiguracion(error);
        }

        List<CrmWhatsappTemplateResponse> utilizables = plantillas.stream()
                .filter(CrmWhatsappTemplateResponse::disponible)
                .filter(plantilla -> !esDeEjemplo(plantilla))
                .toList();
        boolean listo = !utilizables.isEmpty();

        return new WhatsappReengagementGuideResponse(
                listo,
                resumen(listo, problemaDeConfiguracion, utilizables.size()),
                pasos(problemaDeConfiguracion, listo),
                advertencias(plantillas, utilizables, problemaDeConfiguracion),
                MODELO,
                utilizables
        );
    }

    /**
     * Solo se repite el texto de Meta cuando el codigo es de configuracion, que es
     * accionable. Para el resto se responde en generico y el detalle queda en el log,
     * igual que hace {@code ErrorExposurePolicy}.
     */
    private String motivoDeConfiguracion(BusinessException error) {
        return switch (error.getCode()) {
            case "CRM_WHATSAPP_NO_CONFIGURADO" ->
                    "WhatsApp todavia no esta configurado para esta empresa.";
            case "CRM_WHATSAPP_INACTIVO" ->
                    "La integracion de WhatsApp esta desactivada.";
            case "CRM_WHATSAPP_CONFIG_INCOMPLETA" ->
                    "Faltan el Access token o el WABA ID en la configuracion de WhatsApp.";
            default ->
                    "No se pudo consultar el catalogo de plantillas de Meta. "
                            + "Revisa la conexion desde la pantalla de configuracion.";
        };
    }

    private boolean esDeEjemplo(CrmWhatsappTemplateResponse plantilla) {
        return plantilla.nombre() != null
                && PLANTILLAS_DE_EJEMPLO.contains(plantilla.nombre().toLowerCase(Locale.ROOT));
    }

    private String resumen(boolean listo, String problemaDeConfiguracion, int utilizables) {
        if (problemaDeConfiguracion != null) {
            return problemaDeConfiguracion;
        }
        if (!listo) {
            return "Todavia no hay ninguna plantilla aprobada que sirva para reenganchar.";
        }
        return utilizables == 1
                ? "Listo: hay 1 plantilla aprobada para reenganchar."
                : "Listo: hay " + utilizables + " plantillas aprobadas para reenganchar.";
    }

    private List<String> pasos(String problemaDeConfiguracion, boolean listo) {
        List<String> pasos = new ArrayList<>();
        if (problemaDeConfiguracion != null) {
            pasos.add("Completa y prueba la conexion de WhatsApp antes de seguir: "
                    + "necesitas Access token, WABA ID y Phone number ID.");
            return List.copyOf(pasos);
        }
        if (!listo) {
            pasos.add("Crea la plantilla en el Administrador de WhatsApp de Meta, "
                    + "en Plantillas de mensajes.");
            pasos.add("Usala solo con texto: cuerpo obligatorio, y encabezado o pie opcionales. "
                    + "Un encabezado de imagen o un boton de URL con variables la vuelven no enviable "
                    + "desde el CRM.");
            pasos.add("Numera las variables en orden ({{1}}, {{2}}, {{3}}...). "
                    + "El pie de pagina no admite variables.");
            pasos.add("Agrega botones de respuesta rapida. Cuando el cliente toca uno se reabre la "
                    + "ventana de 24 horas y el asesor vuelve a escribir texto libre.");
            pasos.add("Espera la aprobacion de Meta. En cuanto quede aprobada aparece sola en esta "
                    + "lista, sin tocar nada en el CRM.");
        }
        pasos.add("Desde la ficha del prospecto, programa el reenganche eligiendo la plantilla y "
                + "la fecha. Si el prospecto tiene una cotizacion enviada, el CRM llena las "
                + "variables solo.");
        pasos.add("El envio se cancela solo si el cliente responde antes o pide la baja.");
        return List.copyOf(pasos);
    }

    private List<String> advertencias(
            List<CrmWhatsappTemplateResponse> plantillas,
            List<CrmWhatsappTemplateResponse> utilizables,
            String problemaDeConfiguracion) {
        List<String> advertencias = new ArrayList<>();
        if (problemaDeConfiguracion != null) {
            return List.copyOf(advertencias);
        }

        plantillas.stream()
                .filter(this::esDeEjemplo)
                .findFirst()
                .ifPresent(plantilla -> advertencias.add(
                        "La plantilla de ejemplo \"" + plantilla.nombre() + "\" solo se puede enviar "
                                + "desde los numeros de prueba de Meta. Crea una propia."));

        plantillas.stream()
                .filter(plantilla -> !plantilla.disponible() && plantilla.motivoNoDisponible() != null)
                .forEach(plantilla -> advertencias.add(
                        "\"" + plantilla.nombre() + "\" esta aprobada pero no se puede enviar desde "
                                + "el CRM: " + plantilla.motivoNoDisponible()));

        boolean hayUtility = utilizables.stream()
                .anyMatch(plantilla -> "UTILITY".equalsIgnoreCase(plantilla.categoria()));
        if (!utilizables.isEmpty() && !hayUtility) {
            advertencias.add("Todas tus plantillas utilizables son de marketing. Funcionan, pero "
                    + "cuestan mas por envio y Meta puede decidir no entregarlas para cuidar la "
                    + "experiencia del usuario. Una plantilla que cite una cotizacion concreta "
                    + "califica como utility.");
        }
        return List.copyOf(advertencias);
    }
}
