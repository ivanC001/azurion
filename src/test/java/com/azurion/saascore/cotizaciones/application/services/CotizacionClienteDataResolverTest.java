package com.azurion.saascore.cotizaciones.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CotizacionClienteDataResolverTest {

    private final CrmOportunidadRepository oportunidadRepository = mock(CrmOportunidadRepository.class);
    private final CotizacionClienteDataResolver resolver = new CotizacionClienteDataResolver(oportunidadRepository);

    @Test
    void resolvesCrmProspectDataWhenQuoteHasNoConvertedClient() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona("JURIDICA");
        prospecto.setTipoDocumento("6");
        prospecto.setNumeroDocumento("20123456789");
        prospecto.setNombre("Contacto comercial");
        prospecto.setRazonSocial("Empresa Demo SAC");
        prospecto.setCorreo("ventas@demo.test");
        prospecto.setTelefono("999999999");
        prospecto.setDireccion("Av. Principal 123");

        CrmOportunidad oportunidad = new CrmOportunidad();
        oportunidad.setProspecto(prospecto);
        when(oportunidadRepository.findWithRelationsById(15L)).thenReturn(Optional.of(oportunidad));

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCrmOportunidadId(15L);

        CotizacionClienteData result = resolver.resolveForEmission(cotizacion);

        assertThat(result.nombre()).isEqualTo("Empresa Demo SAC");
        assertThat(result.numeroDocumento()).isEqualTo("20123456789");
        assertThat(result.correo()).isEqualTo("ventas@demo.test");
        assertThat(result.telefono()).isEqualTo("999999999");
        assertThat(result.direccion()).isEqualTo("Av. Principal 123");
    }

    @Test
    void prioritizesTheClientLinkedDirectlyToTheQuote() {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento("1");
        cliente.setNumeroDocumento("12345678");
        cliente.setNombre("Ana Perez");
        cliente.setEmail("ana@demo.test");

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setCrmOportunidadId(20L);

        CotizacionClienteData result = resolver.resolveForEmission(cotizacion);

        assertThat(result.nombre()).isEqualTo("Ana Perez");
        assertThat(result.numeroDocumento()).isEqualTo("12345678");
        assertThat(result.correo()).isEqualTo("ana@demo.test");
    }

    @Test
    void rejectsCrmQuoteEmissionWhenIdentityAndContactAreIncomplete() {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setNombre("Contacto sin ficha");

        CrmOportunidad oportunidad = new CrmOportunidad();
        oportunidad.setProspecto(prospecto);
        when(oportunidadRepository.findWithRelationsById(25L)).thenReturn(Optional.of(oportunidad));

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCrmOportunidadId(25L);

        assertThatThrownBy(() -> resolver.resolveForEmission(cotizacion))
                .hasMessageContaining("documento de identidad")
                .hasMessageContaining("telefono o correo");
    }
}
