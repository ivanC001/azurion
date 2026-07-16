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
    private LandingProductMode modoProducto = LandingProductMode.REQUERIDO;

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
}
