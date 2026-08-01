package com.azurion.saascore.cotizaciones.application.services;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CotizacionClienteDataResolver {

    private final CrmOportunidadRepository oportunidadRepository;

    public CotizacionClienteData resolveForEmission(Cotizacion cotizacion) {
        CotizacionClienteData data = resolve(cotizacion);
        if (cotizacion.getCrmOportunidadId() == null) {
            return data;
        }

        List<String> missing = new ArrayList<>();
        if (!hasText(data.nombre())) {
            missing.add("nombre o razon social");
        }
        if (!hasText(data.numeroDocumento())) {
            missing.add("DNI, RUC o documento de identidad");
        }
        if (!hasText(data.telefono()) && !hasText(data.correo())) {
            missing.add("telefono o correo");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(
                    "COTIZACION_CLIENTE_INCOMPLETO",
                    "Completa los datos del cliente antes de emitir la cotizacion: " + String.join(", ", missing) + "."
            );
        }
        return data;
    }

    public CotizacionClienteData resolve(Cotizacion cotizacion) {
        if (cotizacion.getCliente() != null) {
            return fromClient(cotizacion.getCliente());
        }
        if (cotizacion.getCrmOportunidadId() == null) {
            return empty();
        }

        CrmOportunidad oportunidad = oportunidadRepository.findWithRelationsById(cotizacion.getCrmOportunidadId())
                .orElseThrow(() -> new BusinessException(
                        "COTIZACION_OPORTUNIDAD_NO_ENCONTRADA",
                        "No se encontro la oportunidad vinculada a la cotizacion."
                ));
        if (oportunidad.getCliente() != null) {
            return fromClient(oportunidad.getCliente());
        }
        if (oportunidad.getProspecto() != null) {
            return fromProspect(oportunidad.getProspecto());
        }
        return empty();
    }

    private CotizacionClienteData fromClient(Cliente cliente) {
        return new CotizacionClienteData(
                trim(cliente.getNombre()),
                trim(cliente.getTipoDocumento()),
                trim(cliente.getNumeroDocumento()),
                trim(cliente.getEmail()),
                trim(cliente.getTelefono()),
                trim(cliente.getDireccion())
        );
    }

    private CotizacionClienteData fromProspect(CrmProspecto prospecto) {
        String nombre = firstText(
                prospecto.getRazonSocial(),
                prospecto.getNombreComercial(),
                prospecto.getNombre()
        );
        return new CotizacionClienteData(
                nombre,
                trim(prospecto.getTipoDocumento()),
                trim(prospecto.getNumeroDocumento()),
                trim(prospecto.getCorreo()),
                trim(prospecto.getTelefono()),
                trim(prospecto.getDireccion())
        );
    }

    private CotizacionClienteData empty() {
        return new CotizacionClienteData(null, null, null, null, null, null);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trim(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
