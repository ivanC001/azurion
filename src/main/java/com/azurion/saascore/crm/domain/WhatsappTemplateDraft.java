package com.azurion.saascore.crm.domain;

import com.azurion.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Plantilla que el usuario compone en el CRM para mandar a revision de Meta.
 *
 * <p>Valida las mismas reglas que {@link WhatsappTemplate} y el parser aplican al
 * leer el catalogo. Ese es el punto: sin esta simetria se puede aprobar en Meta una
 * plantilla que despues el compositor no sabe enviar, y el usuario se entera una
 * semana mas tarde y sin explicacion, que es exactamente lo que pasaba cuando habia
 * que crearlas a mano en el Administrador de WhatsApp.
 *
 * @param buttons botones estaticos; los de URL no admiten variables
 */
public record WhatsappTemplateDraft(
        String name,
        String languageCode,
        String category,
        Component header,
        Component body,
        String footer,
        List<Button> buttons) {

    /** Meta solo admite minusculas, digitos y guion bajo en el nombre. */
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,512}");
    private static final Set<String> CATEGORIES = Set.of("MARKETING", "UTILITY");
    private static final Set<String> BUTTON_TYPES = Set.of("QUICK_REPLY", "URL", "PHONE_NUMBER");
    private static final int MAX_VARIABLES = 30;
    private static final int MAX_BUTTONS = 10;

    /**
     * Un componente con texto y los ejemplos de sus variables, en orden.
     *
     * <p>Meta exige el ejemplo para cada variable: sin el rechaza la plantilla.
     */
    public record Component(String text, List<String> examples) {
        public List<String> parameters() {
            return text == null ? List.of() : WhatsappTemplate.parameterNames(text);
        }
    }

    public record Button(String type, String text, String url, String phoneNumber) {}

    /**
     * Devuelve el borrador normalizado, o falla con el motivo concreto.
     */
    public WhatsappTemplateDraft validated() {
        String normalizedName = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(normalizedName).matches()) {
            throw invalid("El nombre solo admite minusculas, numeros y guion bajo, sin espacios.");
        }
        if (languageCode == null || languageCode.isBlank()) {
            throw invalid("Elige el idioma de la plantilla.");
        }
        String normalizedCategory = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalizedCategory)) {
            throw invalid("La categoria debe ser UTILITY o MARKETING.");
        }
        if (body == null || body.text() == null || body.text().isBlank()) {
            throw invalid("El cuerpo del mensaje es obligatorio.");
        }

        Component normalizedHeader = validateComponent(header, "encabezado");
        Component normalizedBody = validateComponent(body, "cuerpo");
        String normalizedFooter = footer == null || footer.isBlank() ? null : footer.trim();
        if (normalizedFooter != null && !WhatsappTemplate.parameterNames(normalizedFooter).isEmpty()) {
            throw invalid("El pie de pagina no admite variables.");
        }

        int variables = (normalizedHeader == null ? 0 : normalizedHeader.parameters().size())
                + normalizedBody.parameters().size();
        if (variables > MAX_VARIABLES) {
            throw invalid("La plantilla no puede tener mas de " + MAX_VARIABLES + " variables.");
        }

        return new WhatsappTemplateDraft(
                normalizedName,
                languageCode.trim(),
                normalizedCategory,
                normalizedHeader,
                normalizedBody,
                normalizedFooter,
                validateButtons()
        );
    }

    /**
     * Las variables de cada componente se numeran por separado y arrancan en 1, que es
     * como las modela Meta y como las lee el parser al enviar.
     */
    private Component validateComponent(Component component, String label) {
        if (component == null || component.text() == null || component.text().isBlank()) {
            return null;
        }
        String text = component.text().trim();
        List<String> parameters = WhatsappTemplate.parameterNames(text);

        String remaining = text;
        for (String parameter : parameters) {
            remaining = remaining.replace("{{" + parameter + "}}", "");
        }
        if (remaining.contains("{{") || remaining.contains("}}")) {
            throw invalid("Hay una variable mal escrita en el " + label
                    + ". Usa la forma {{1}}, {{2}}, sin espacios dentro de las llaves.");
        }
        for (int index = 0; index < parameters.size(); index++) {
            if (!String.valueOf(index + 1).equals(parameters.get(index))) {
                throw invalid("Las variables del " + label + " deben ir numeradas en orden "
                        + "y sin saltos: {{1}}, {{2}}, {{3}}...");
            }
        }

        List<String> examples = component.examples() == null ? List.of() : component.examples();
        if (examples.size() != parameters.size()) {
            throw invalid("Meta exige un ejemplo por variable. El " + label + " tiene "
                    + parameters.size() + " variables y " + examples.size() + " ejemplos.");
        }
        List<String> normalizedExamples = new ArrayList<>();
        for (String example : examples) {
            if (example == null || example.isBlank()) {
                throw invalid("Completa todos los ejemplos del " + label + ".");
            }
            // Un ejemplo con saltos de linea hace que Meta rechace la plantilla entera.
            normalizedExamples.add(example.replaceAll("\\s+", " ").trim());
        }
        return new Component(text, List.copyOf(normalizedExamples));
    }

    private List<Button> validateButtons() {
        if (buttons == null || buttons.isEmpty()) {
            return List.of();
        }
        if (buttons.size() > MAX_BUTTONS) {
            throw invalid("Una plantilla admite hasta " + MAX_BUTTONS + " botones.");
        }
        List<Button> normalized = new ArrayList<>();
        Set<String> labels = new LinkedHashSet<>();
        for (Button button : buttons) {
            String type = button.type() == null ? "" : button.type().trim().toUpperCase(Locale.ROOT);
            if (!BUTTON_TYPES.contains(type)) {
                throw invalid("Los botones solo pueden ser de respuesta rapida, enlace o telefono.");
            }
            String text = button.text() == null ? "" : button.text().trim();
            if (text.isEmpty()) {
                throw invalid("Todos los botones necesitan un texto.");
            }
            if (!labels.add(text.toLowerCase(Locale.ROOT))) {
                throw invalid("Hay dos botones con el mismo texto: \"" + text + "\".");
            }
            String url = button.url() == null ? null : button.url().trim();
            String phone = button.phoneNumber() == null ? null : button.phoneNumber().trim();
            if ("URL".equals(type)) {
                if (url == null || url.isEmpty()) {
                    throw invalid("El boton \"" + text + "\" necesita una direccion web.");
                }
                if (url.contains("{{")) {
                    // El compositor no sabe rellenar una URL con variables al enviar.
                    throw invalid("El enlace del boton \"" + text + "\" no puede llevar variables.");
                }
            }
            if ("PHONE_NUMBER".equals(type) && (phone == null || phone.isEmpty())) {
                throw invalid("El boton \"" + text + "\" necesita un numero de telefono.");
            }
            normalized.add(new Button(type, text, url, phone));
        }
        return List.copyOf(normalized);
    }

    private BusinessException invalid(String message) {
        return new BusinessException("CRM_WHATSAPP_PLANTILLA_BORRADOR_INVALIDO", message);
    }
}
