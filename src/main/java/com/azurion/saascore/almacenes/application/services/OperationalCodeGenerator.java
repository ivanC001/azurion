package com.azurion.saascore.almacenes.application.services;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationalCodeGenerator {

    private static final int MAX_SEQUENCE = 9_999;

    private final SucursalRepository sucursalRepository;
    private final AlmacenRepository almacenRepository;

    public String nextSucursalCode() {
        Set<String> usedCodes = sucursalRepository.findAllByOrderByNombreAsc().stream()
                .map(Sucursal::getCodigo)
                .map(this::upper)
                .collect(Collectors.toSet());
        return nextAvailable("SUC-", 3, usedCodes);
    }

    public String nextAlmacenCode(Sucursal sucursal) {
        String branchCode = sanitizeSegment(sucursal.getCodigo());
        String prefix = "ALM-" + branchCode + "-";
        Set<String> usedCodes = almacenRepository.findAll().stream()
                .map(Almacen::getCodigo)
                .map(this::upper)
                .collect(Collectors.toSet());
        return nextAvailable(prefix, 2, usedCodes);
    }

    private String nextAvailable(String prefix, int padding, Set<String> usedCodes) {
        for (int sequence = 1; sequence <= MAX_SEQUENCE; sequence++) {
            String candidate = prefix + String.format(Locale.ROOT, "%0" + padding + "d", sequence);
            if (!usedCodes.contains(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No hay codigos automaticos disponibles para " + prefix);
    }

    private String sanitizeSegment(String value) {
        String normalized = Normalizer.normalize(value == null ? "SUC" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) {
            normalized = "SUC";
        }
        return normalized.substring(0, Math.min(normalized.length(), 35));
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
