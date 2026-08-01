package com.azurion.saascore.cotizaciones.application.services;

public record CotizacionClienteData(
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        String correo,
        String telefono,
        String direccion
) {
}
