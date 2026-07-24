package com.azurion.saascore.planes.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "planes", schema = "public")
public class Plan extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "descripcion", length = 400)
    private String descripcion;

    @Column(name = "limite_mensual_bolsa", nullable = false)
    private Long limiteMensualBolsa;

    @Column(name = "limite_usuarios", nullable = false)
    private Integer limiteUsuarios;

    @Column(name = "precio_mensual", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}
