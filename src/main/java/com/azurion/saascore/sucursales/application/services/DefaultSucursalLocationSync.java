package com.azurion.saascore.sucursales.application.services;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Alinea la sede base del tenant con el domicilio fiscal de la empresa.
 *
 * La sede base la crean las migraciones del esquema (V9/V43), que se ejecutan
 * antes de que exista la empresa y por eso siembran una direccion peruana fija
 * ('150101 LIMA'). Para un tenant de otro pais eso es un dato inventado.
 *
 * Cuando el usuario completa el perfil fiscal, esta sincronizacion traslada esa
 * ubicacion a la sede base. Solo actua fuera de Peru y solo sobre la sede
 * generada automaticamente: una sucursal que alguien configuro a mano no se
 * toca nunca.
 */
@Service
@RequiredArgsConstructor
public class DefaultSucursalLocationSync {

    private static final String PERU = "PE";

    /** Codigo de la sede que siembran las migraciones del esquema. */
    private static final String DEFAULT_CODE = "SUC-PRINCIPAL";

    /** Ubigeo de Lima que esas migraciones dejan como marcador. */
    private static final String SEEDED_UBIGEO = "150101";

    private final SucursalRepository sucursalRepository;

    public void syncFromEmpresa(Empresa empresa) {
        if (empresa == null || isPeru(empresa.getPaisCodigo())) {
            return;
        }

        sucursalRepository.findAll().stream()
                .filter(this::isSeededBranch)
                .findFirst()
                .ifPresent(sucursal -> applyEmpresaLocation(sucursal, empresa));
    }

    /**
     * Solo la sede generada por migracion, y solo mientras conserve el ubigeo
     * sembrado: si ya la editaron, su ubicacion es intencionada.
     */
    private boolean isSeededBranch(Sucursal sucursal) {
        return DEFAULT_CODE.equalsIgnoreCase(sucursal.getCodigo())
                && SEEDED_UBIGEO.equals(sucursal.getUbigeoCodigo());
    }

    private void applyEmpresaLocation(Sucursal sucursal, Empresa empresa) {
        String fallback = firstNonBlank(empresa.getPaisNombre(), "Sin especificar");

        // Fuera de Peru el ubigeo SUNAT no aplica.
        sucursal.setUbigeoCodigo(null);
        sucursal.setDepartamento(firstNonBlank(empresa.getDepartamento(), fallback));
        sucursal.setProvincia(firstNonBlank(empresa.getProvincia(), fallback));
        sucursal.setDistrito(firstNonBlank(empresa.getDistrito(), fallback));

        if (isBlank(sucursal.getDireccion()) || "Generado por migracion".equals(sucursal.getDireccion())) {
            sucursal.setDireccion(firstNonBlank(empresa.getDireccionFiscal(), sucursal.getDireccion()));
        }

        sucursalRepository.save(sucursal);
    }

    private boolean isPeru(String paisCodigo) {
        return paisCodigo == null
                || paisCodigo.isBlank()
                || PERU.equals(paisCodigo.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
