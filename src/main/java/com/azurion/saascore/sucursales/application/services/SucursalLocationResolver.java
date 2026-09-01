package com.azurion.saascore.sucursales.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.ubigeos.domain.entities.Ubigeo;
import com.azurion.saascore.ubigeos.domain.repositories.UbigeoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resuelve la ubicacion de una sucursal segun el pais del tenant.
 *
 * El ubigeo es un codigo de SUNAT y solo existe para Peru: exigirlo a un tenant
 * de otro pais obligaba a inventar una direccion peruana. Para esos tenants la
 * ubicacion es texto libre y, si no se indica, se hereda del perfil fiscal de
 * la empresa.
 *
 * Para Peru la validacion se mantiene intacta: sin ubigeo valido no se puede
 * emitir comprobantes electronicos.
 */
@Service
@RequiredArgsConstructor
public class SucursalLocationResolver {

    private static final String PERU = "PE";

    private static final int UBIGEO_LENGTH = 6;

    private final UbigeoRepository ubigeoRepository;

    private final EmpresaRepository empresaRepository;

    /**
     * Ubicacion resuelta de una sucursal. `ubigeoCodigo` es null fuera de Peru.
     */
    public record SucursalLocation(
            String ubigeoCodigo,
            String departamento,
            String provincia,
            String distrito
    ) {
    }

    public boolean requiresUbigeo() {
        return PERU.equals(tenantCountryCode());
    }

    /**
     * @param ubigeoCodigo codigo SUNAT; obligatorio solo en Peru
     * @param departamento ubicacion libre para tenants de otro pais
     */
    public SucursalLocation resolve(
            String ubigeoCodigo,
            String departamento,
            String provincia,
            String distrito
    ) {
        if (requiresUbigeo()) {
            return fromUbigeoCatalog(ubigeoCodigo);
        }
        return fromFreeText(departamento, provincia, distrito);
    }

    /**
     * En Peru la ubicacion la dicta el catalogo de SUNAT, no el usuario: asi el
     * comprobante electronico siempre lleva un ubigeo declarable.
     */
    private SucursalLocation fromUbigeoCatalog(String ubigeoCodigo) {
        String sanitized = sanitize(ubigeoCodigo);
        if (sanitized == null) {
            throw new BusinessException("UBIGEO_REQUERIDO", "Selecciona el ubigeo SUNAT de la sucursal");
        }

        Ubigeo ubigeo = ubigeoRepository.findByCodigo(sanitized)
                .orElseThrow(() -> new BusinessException(
                        "UBIGEO_NO_ENCONTRADO",
                        "No existe ubigeo SUNAT: " + sanitized));

        return new SucursalLocation(
                ubigeo.getCodigo(),
                ubigeo.getDepartamento(),
                ubigeo.getProvincia(),
                ubigeo.getDistrito()
        );
    }

    /**
     * Fuera de Peru se toma lo que indique el usuario y, en su defecto, el
     * domicilio fiscal de la empresa. Si tampoco esta, se usa el nombre del
     * pais para no dejar la sucursal sin ubicacion.
     */
    private SucursalLocation fromFreeText(String departamento, String provincia, String distrito) {
        Empresa empresa = currentEmpresa();
        String fallback = firstNonBlank(
                empresa == null ? null : empresa.getPaisNombre(),
                "Sin especificar"
        );

        return new SucursalLocation(
                null,
                firstNonBlank(departamento, empresa == null ? null : empresa.getDepartamento(), fallback),
                firstNonBlank(provincia, empresa == null ? null : empresa.getProvincia(), fallback),
                firstNonBlank(distrito, empresa == null ? null : empresa.getDistrito(), fallback)
        );
    }

    private String tenantCountryCode() {
        Empresa empresa = currentEmpresa();
        String code = empresa == null ? null : empresa.getPaisCodigo();
        return code == null || code.isBlank()
                ? PERU
                : code.trim().toUpperCase(Locale.ROOT);
    }

    private Empresa currentEmpresa() {
        return empresaRepository.findByTenantId(TenantContext.getTenantId()).orElse(null);
    }

    private String sanitize(String value) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.length() != UBIGEO_LENGTH) {
            return null;
        }
        return trimmed;
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
