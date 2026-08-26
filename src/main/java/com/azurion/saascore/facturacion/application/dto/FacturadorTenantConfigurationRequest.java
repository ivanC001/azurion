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
    private String cuentas_bancarias_json;
    private MultipartFile logo_file;
    private MultipartFile certificado_file;

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getBusiness_name() {
        return business_name;
    }

    public void setBusiness_name(String business_name) {
        this.business_name = business_name;
    }

    public String getExternal_tenant_id() {
        return external_tenant_id;
    }

    public void setExternal_tenant_id(String external_tenant_id) {
        this.external_tenant_id = external_tenant_id;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getTax_id() {
        return tax_id;
    }

    public void setTax_id(String tax_id) {
        this.tax_id = tax_id;
    }

    public String getSunat_mode() {
        return sunat_mode;
    }

    public void setSunat_mode(String sunat_mode) {
        this.sunat_mode = sunat_mode;
    }

    public String getApi_client_name() {
        return api_client_name;
    }

    public void setApi_client_name(String api_client_name) {
        this.api_client_name = api_client_name;
    }

    public String getRuc_sol() {
        return ruc_sol;
    }

    public void setRuc_sol(String ruc_sol) {
        this.ruc_sol = ruc_sol;
    }

    public String getUsuario_sol() {
        return usuario_sol;
    }

    public void setUsuario_sol(String usuario_sol) {
        this.usuario_sol = usuario_sol;
    }

    public String getClave_sol() {
        return clave_sol;
    }

    public void setClave_sol(String clave_sol) {
        this.clave_sol = clave_sol;
    }

    public String getCertificado_password() {
        return certificado_password;
    }

    public void setCertificado_password(String certificado_password) {
        this.certificado_password = certificado_password;
    }

    public String getSerie_factura() {
        return serie_factura;
    }

    public void setSerie_factura(String serie_factura) {
        this.serie_factura = serie_factura;
    }

    public String getSerie_boleta() {
        return serie_boleta;
    }

    public void setSerie_boleta(String serie_boleta) {
        this.serie_boleta = serie_boleta;
    }

    public String getSerie_nc() {
        return serie_nc;
    }

    public void setSerie_nc(String serie_nc) {
        this.serie_nc = serie_nc;
    }

    public String getSerie_nd() {
        return serie_nd;
    }

    public void setSerie_nd(String serie_nd) {
        this.serie_nd = serie_nd;
    }

    public String getSerie_guia() {
        return serie_guia;
    }

    public void setSerie_guia(String serie_guia) {
        this.serie_guia = serie_guia;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getCuentas_bancarias_json() {
        return cuentas_bancarias_json;
    }

    public void setCuentas_bancarias_json(String cuentas_bancarias_json) {
        this.cuentas_bancarias_json = cuentas_bancarias_json;
    }

    public MultipartFile getLogo_file() {
        return logo_file;
    }

    public void setLogo_file(MultipartFile logo_file) {
        this.logo_file = logo_file;
    }

    public MultipartFile getCertificado_file() {
        return certificado_file;
    }

    public void setCertificado_file(MultipartFile certificado_file) {
        this.certificado_file = certificado_file;
    }
}
