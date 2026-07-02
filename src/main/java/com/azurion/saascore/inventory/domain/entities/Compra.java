package com.azurion.saascore.inventory.domain.entities;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "compras")
public class Compra extends BaseEntity {

    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(name = "proveedor_documento", length = 20)
    private String proveedorDocumento;

    @Column(name = "proveedor_nombre", length = 255)
    private String proveedorNombre;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private String tipoComprobante;

    @Column(name = "serie", length = 20)
    private String serie;

    @Column(name = "correlativo", length = 30)
    private String correlativo;

    @Column(name = "numero_comprobante", nullable = false, length = 60)
    private String numeroComprobante;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_ingreso", nullable = false)
    private OffsetDateTime fechaIngreso = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "total", nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "REGISTRADA";

    @OneToMany(mappedBy = "compra")
    private List<CompraDetalle> detalles = new ArrayList<>();
}
