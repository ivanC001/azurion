package com.azurion.saascore.clientes.domain.entities;

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
@Table(name = "clientes")
public class Cliente extends BaseEntity {

    @Column(name = "tipo_documento", nullable = false, length = 2)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "direccion", length = 500)
    private String direccion;

    @Column(name = "ubigeo", length = 6)
    private String ubigeo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "limite_credito", nullable = false, precision = 18, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @Column(name = "saldo_deuda", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoDeuda = BigDecimal.ZERO;

    @Column(name = "dias_credito", nullable = false)
    private Integer diasCredito = 0;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
