package com.azurion.saascore.configuracion.domain.entities;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "empresa_modulos", schema = "public")
public class EmpresaModulo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuracion_extra", columnDefinition = "jsonb")
    private String configuracionExtra;
}
