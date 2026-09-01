package com.azurion.saascore.crm.infrastructure.http;

import com.azurion.saascore.crm.domain.WhatsappTemplate;
import com.azurion.saascore.crm.domain.WhatsappTemplate.Component;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class WhatsappTemplateParser {
    private WhatsappTemplateParser() {}

    static List<WhatsappTemplate> approved(JsonNode response) {
        List<WhatsappTemplate> templates = new ArrayList<>();
        for (JsonNode template : response.path("data")) {
            if (!"APPROVED".equalsIgnoreCase(template.path("status").asText())) {
                continue;
            }
            String name = template.path("name").asText("");
            String language = template.path("language").asText("");
            if (name.isBlank() || language.isBlank()) {
                continue;
            }
            List<Component> components = new ArrayList<>();
            Set<String> types = new HashSet<>();
            boolean compatible = true;
            for (JsonNode component : template.path("components")) {
                String type = component.path("type").asText("").toUpperCase(Locale.ROOT);
                compatible &= types.add(type);
                String text = component.path("text").asText("");
                List<String> parameters = WhatsappTemplate.parameterNames(text);
                if (Set.of("HEADER", "BODY", "FOOTER").contains(type)) {
                    boolean textFormat = "TEXT".equalsIgnoreCase(component.path("format").asText("TEXT"));
                    compatible &= textFormat && validParameters(text, parameters)
                            && (!"FOOTER".equals(type) || parameters.isEmpty());
                    components.add(new Component(type, text, parameters));
                } else if ("BUTTONS".equals(type)) {
                    List<String> labels = new ArrayList<>();
                    for (JsonNode button : component.path("buttons")) {
                        compatible &= Set.of("URL", "PHONE_NUMBER", "QUICK_REPLY")
                                .contains(button.path("type").asText(""))
                                && !button.path("url").asText("").contains("{{");
                        labels.add(button.path("text").asText(""));
                    }
                    components.add(new Component(type, String.join(" | ", labels), List.of()));
                } else {
                    compatible = false;
                }
            }
            List<String> order = List.of("HEADER", "BODY", "FOOTER", "BUTTONS");
            components.sort(java.util.Comparator.comparingInt(c -> order.indexOf(c.type())));
            compatible &= components.stream().anyMatch(c -> "BODY".equals(c.type()) && !c.text().isBlank());
            compatible &= components.stream().mapToInt(c -> c.parameters().size()).sum() <= 30;
            templates.add(new WhatsappTemplate(template.path("id").asText(null), name, language,
                    "APPROVED", template.path("category").asText(""), List.copyOf(components),
                    compatible ? null : "Esta plantilla requiere componentes que el compositor de texto no admite."));
        }
        return List.copyOf(templates);
    }

    private static boolean validParameters(String text, List<String> names) {
        String remaining = text;
        for (String name : names) {
            remaining = remaining.replace("{{" + name + "}}", "");
        }
        if (remaining.contains("{{") || remaining.contains("}}")) {
            return false;
        }
        boolean numeric = names.stream().anyMatch(name -> name.matches("[0-9]+"));
        if (numeric) {
            for (int i = 0; i < names.size(); i++) {
                if (!String.valueOf(i + 1).equals(names.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }
}
