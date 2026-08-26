package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_landing_config")
public class CrmLandingConfig extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    @Column(name = "landing_key", nullable = false, unique = true, length = 120)
    private String landingKey;

    @Column(name = "campania", length = 120)
    private String campania;

    @Column(name = "canal_ingreso", nullable = false, length = 30)
    private String canalIngreso = "LANDING";

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "recibir_leads", nullable = false)
    private boolean recibirLeads = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_producto", nullable = false, length = 30)
    private LandingProductMode modoProducto = LandingProductMode.OPCIONAL;

    @Column(name = "crear_seguimiento", nullable = false)
    private boolean crearSeguimiento = true;

    @Column(name = "crear_actividad_inicial", nullable = false)
    private boolean crearActividadInicial = true;

    @Column(name = "responsable_id", length = 80)
    private String responsableId;

    @Column(name = "campos_obligatorios", columnDefinition = "TEXT")
    private String camposObligatorios;

    @Column(name = "validar_duplicados_por", nullable = false, length = 40)
    private String validarDuplicadosPor = "TELEFONO_CORREO";

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getLandingKey() {
        return landingKey;
    }

    public void setLandingKey(String landingKey) {
        this.landingKey = landingKey;
    }

    public String getCampania() {
        return campania;
    }

    public void setCampania(String campania) {
        this.campania = campania;
    }

    public String getCanalIngreso() {
        return canalIngreso;
    }

    public void setCanalIngreso(String canalIngreso) {
        this.canalIngreso = canalIngreso;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public boolean isRecibirLeads() {
        return recibirLeads;
    }

    public void setRecibirLeads(boolean recibirLeads) {
        this.recibirLeads = recibirLeads;
    }

    public LandingProductMode getModoProducto() {
        return modoProducto;
    }

    public void setModoProducto(LandingProductMode modoProducto) {
        this.modoProducto = modoProducto;
    }

    public boolean isCrearSeguimiento() {
        return crearSeguimiento;
    }

    public void setCrearSeguimiento(boolean crearSeguimiento) {
        this.crearSeguimiento = crearSeguimiento;
    }

    public boolean isCrearActividadInicial() {
        return crearActividadInicial;
    }

    public void setCrearActividadInicial(boolean crearActividadInicial) {
        this.crearActividadInicial = crearActividadInicial;
    }

    public String getResponsableId() {
        return responsableId;
    }

    public void setResponsableId(String responsableId) {
        this.responsableId = responsableId;
    }

    public String getCamposObligatorios() {
        return camposObligatorios;
    }

    public void setCamposObligatorios(String camposObligatorios) {
        this.camposObligatorios = camposObligatorios;
    }

    public String getValidarDuplicadosPor() {
        return validarDuplicadosPor;
    }

    public void setValidarDuplicadosPor(String validarDuplicadosPor) {
        this.validarDuplicadosPor = validarDuplicadosPor;
    }
}
