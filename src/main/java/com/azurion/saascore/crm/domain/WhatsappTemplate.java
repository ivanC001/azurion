package com.azurion.saascore.crm.domain;

import com.azurion.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record WhatsappTemplate(
        String id,
        String name,
        String languageCode,
        String status,
        String category,
        List<Component> components,
        String unavailableReason) {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z_0-9]*|[1-9][0-9]*)}}");

    public record Component(String type, String text, List<String> parameters) {}
    public record Variable(String component, String name) {}

    public boolean available() {
        return "APPROVED".equals(status) && unavailableReason == null;
    }

    public String bodyText() {
        return components.stream().filter(c -> "BODY".equals(c.type()))
                .map(Component::text).findFirst().orElse("");
    }

    public List<Variable> variables() {
        return components.stream().flatMap(c -> c.parameters().stream()
                .map(name -> new Variable(c.type(), name))).toList();
    }

    public int parameterCount() {
        return variables().size();
    }

    public static List<String> parameterNames(String text) {
        List<String> names = PLACEHOLDER.matcher(text).results()
                .map(match -> match.group(1)).distinct().toList();
        if (names.stream().allMatch(name -> name.matches("[1-9][0-9]*"))) {
            return names.stream().sorted(java.util.Comparator.comparingInt(String::length)
                    .thenComparing(java.util.Comparator.naturalOrder())).toList();
        }
        return names;
    }

    public List<String> validateParameters(List<String> values) {
        if (!available()) {
            throw new BusinessException("CRM_WHATSAPP_PLANTILLA_NO_DISPONIBLE",
                    unavailableReason == null ? "La plantilla ya no esta aprobada" : unavailableReason);
        }
        List<Variable> variables = variables();
        if (values == null || values.size() != variables.size()) {
            throw new BusinessException("CRM_WHATSAPP_PARAMETROS_PLANTILLA_INVALIDOS",
                    "La plantilla requiere " + variables.size() + " parametros, pero se recibieron "
                            + (values == null ? 0 : values.size()));
        }
        List<String> normalized = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            Variable variable = variables.get(index);
            String reference = "{{" + variable.name() + "}} (" + variable.component() + ")";
            if (value == null || value.isBlank()) {
                throw new BusinessException("CRM_WHATSAPP_PARAMETRO_VACIO",
                        "Falta el valor de " + reference + ". Completa la variable antes de enviar.");
            }
            String trimmed = value.trim();
            if (trimmed.length() > 1024 || trimmed.contains("{{") || trimmed.contains("}}")
                    || trimmed.chars().anyMatch(Character::isISOControl)) {
                throw new BusinessException("CRM_WHATSAPP_PARAMETRO_INVALIDO",
                        "Revisa " + reference + ": usa texto de hasta 1024 caracteres, sin variables ni saltos de linea.");
            }
            normalized.add(trimmed);
        }
        return List.copyOf(normalized);
    }

    public String render(List<String> values) {
        List<String> rendered = new ArrayList<>();
        int offset = 0;
        for (Component component : components) {
            Map<String, String> replacements = new LinkedHashMap<>();
            for (String parameter : component.parameters()) {
                replacements.put(parameter, values.get(offset++));
            }
            String text = PLACEHOLDER.matcher(component.text()).replaceAll(match ->
                    java.util.regex.Matcher.quoteReplacement(replacements.getOrDefault(match.group(1), match.group())));
            if (!text.isBlank()) {
                rendered.add(text);
            }
        }
        return rendered.stream().collect(Collectors.joining("\n\n"));
    }
}
