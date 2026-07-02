package com.azurion.saascore.tributacion.application.services;

import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TaxConfigurationValidator {
    private static final Set<String> AFECTACIONES_SIN_IGV = Set.of(
            "20", "21", "30", "31", "32", "33", "34", "35", "36", "40"
    );

    public void validate(String afectacion, String tributo, BigDecimal porcentaje, boolean required) {
        if (!required && isBlank(afectacion) && isBlank(tributo) && porcentaje == null) {
            return;
        }
        if (isBlank(afectacion)) {
            throw new BusinessException("AFECTACION_TRIBUTARIA_REQUERIDA", "Selecciona el tipo de afectacion tributaria");
        }
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("PORCENTAJE_TRIBUTARIO_INVALIDO", "El porcentaje tributario debe estar entre 0 y 100");
        }
        if (AFECTACIONES_SIN_IGV.contains(afectacion) && porcentaje.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("PORCENTAJE_TRIBUTARIO_INVALIDO", "Productos exonerados, inafectos o de exportacion deben usar impuesto 0");
        }
        if (isBlank(tributo)) {
            throw new BusinessException("TRIBUTO_REQUERIDO", "Selecciona el tributo correspondiente a la afectacion");
        }
    }

    public void validateProducto(boolean afectoIgv, String afectacion, String tributo, BigDecimal porcentaje) {
        validate(afectacion, tributo, porcentaje, true);
        if (!afectoIgv && porcentaje.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("PRODUCTO_NO_AFECTO_CON_IGV", "Un producto no afecto debe usar impuesto 0");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
