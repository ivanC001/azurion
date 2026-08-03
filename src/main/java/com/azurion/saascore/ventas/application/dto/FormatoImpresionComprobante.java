package com.azurion.saascore.ventas.application.dto;

public enum FormatoImpresionComprobante {
    A4("a4", "A4"),
    TICKET("ticket", "80mm");

    private final String facturadorValue;
    private final String filenameSuffix;

    FormatoImpresionComprobante(String facturadorValue, String filenameSuffix) {
        this.facturadorValue = facturadorValue;
        this.filenameSuffix = filenameSuffix;
    }

    public String facturadorValue() {
        return facturadorValue;
    }

    public String filenameSuffix() {
        return filenameSuffix;
    }
}
