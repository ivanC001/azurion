UPDATE productos
SET usa_configuracion_empresa = FALSE,
    tipo_afectacion_igv_id = '20',
    tributo_id = '9997',
    porcentaje_impuesto = 0.00
WHERE afecto_igv = FALSE;
