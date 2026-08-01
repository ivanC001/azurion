package com.azurion.saascore.facturacion.application.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class FacturadorTenantConfigurationRequest {

    private String ruc;
    private String business_name;
    private String external_tenant_id;
    private String country_code;
    private String tax_id;
    private String sunat_mode;
    private String api_client_name;
    private String ruc_sol;
    private String usuario_sol;
    private String clave_sol;
    private String certificado_password;
    private String serie_factura;
    private String serie_boleta;
    private String serie_nc;
    private String serie_nd;
    private String serie_guia;
    private BigDecimal igv;
    private String moneda;
    private MultipartFile logo_file;
    private MultipartFile certificado_file;
}
