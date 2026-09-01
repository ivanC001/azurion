package com.azurion.saascore.sucursales.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.sucursales.application.services.SucursalLocationResolver.SucursalLocation;
import com.azurion.saascore.ubigeos.domain.entities.Ubigeo;
import com.azurion.saascore.ubigeos.domain.repositories.UbigeoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SucursalLocationResolverTest {

    @Mock
    UbigeoRepository ubigeoRepository;

    @Mock
    EmpresaRepository empresaRepository;

    SucursalLocationResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SucursalLocationResolver(ubigeoRepository, empresaRepository);
        TenantContext.setTenantId("tenant-demo");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void tenantFrom(String paisCodigo, String paisNombre) {
        Empresa empresa = new Empresa();
        empresa.setPaisCodigo(paisCodigo);
        empresa.setPaisNombre(paisNombre);
        empresa.setDepartamento("Antioquia");
        empresa.setProvincia("Medellin");
        empresa.setDistrito("El Poblado");
        lenient().when(empresaRepository.findByTenantId("tenant-demo")).thenReturn(Optional.of(empresa));
    }

    private Ubigeo ubigeo() {
        Ubigeo ubigeo = new Ubigeo();
        ubigeo.setCodigo("150101");
        ubigeo.setDepartamento("LIMA");
        ubigeo.setProvincia("LIMA");
        ubigeo.setDistrito("LIMA");
        return ubigeo;
    }

    @Test
    void enPeruLaUbicacionLaDictaElCatalogoDeSunat() {
        tenantFrom("PE", "Peru");
        when(ubigeoRepository.findByCodigo("150101")).thenReturn(Optional.of(ubigeo()));

        // Se ignora lo que escriba el usuario: el comprobante necesita el
        // ubigeo declarable, no un texto libre.
        SucursalLocation location = resolver.resolve("150101", "Inventado", "Inventado", "Inventado");

        assertEquals("150101", location.ubigeoCodigo());
        assertEquals("LIMA", location.departamento());
        assertEquals("LIMA", location.distrito());
    }

    @Test
    void enPeruElUbigeoEsObligatorio() {
        tenantFrom("PE", "Peru");

        BusinessException error =
                assertThrows(BusinessException.class, () -> resolver.resolve(null, null, null, null));

        assertEquals("UBIGEO_REQUERIDO", error.getCode());
    }

    @Test
    void enPeruUnUbigeoInexistenteSeRechaza() {
        tenantFrom("PE", "Peru");
        when(ubigeoRepository.findByCodigo("999999")).thenReturn(Optional.empty());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> resolver.resolve("999999", null, null, null));

        assertEquals("UBIGEO_NO_ENCONTRADO", error.getCode());
    }

    @Test
    void fueraDePeruSeAceptaUbicacionLibreSinUbigeo() {
        tenantFrom("CO", "Colombia");

        SucursalLocation location = resolver.resolve(null, "Cundinamarca", "Bogota", "Chapinero");

        assertNull(location.ubigeoCodigo());
        assertEquals("Cundinamarca", location.departamento());
        assertEquals("Bogota", location.provincia());
        assertEquals("Chapinero", location.distrito());
        verify(ubigeoRepository, never()).findByCodigo(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void fueraDePeruSeHeredaElDomicilioFiscalCuandoNoSeIndicaNada() {
        tenantFrom("CO", "Colombia");

        SucursalLocation location = resolver.resolve(null, null, null, null);

        assertEquals("Antioquia", location.departamento());
        assertEquals("Medellin", location.provincia());
        assertEquals("El Poblado", location.distrito());
    }

    @Test
    void unTenantSinEmpresaRegistradaSeTrataComoPeru() {
        // Si no se puede saber el pais, se conserva la validacion mas estricta.
        when(empresaRepository.findByTenantId("tenant-demo")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> resolver.resolve(null, null, null, null));
    }
}
