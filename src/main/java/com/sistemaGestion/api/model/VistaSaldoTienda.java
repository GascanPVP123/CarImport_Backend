package com.sistemaGestion.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "vista_saldos_tiendas")
public class VistaSaldoTienda {

    @Id
    @Column(name = "tienda_id")
    private Long tiendaId;

    @Column(name = "tienda_nombre")
    private String tiendaNombre;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "total_debe")
    private BigDecimal totalDebe;

    @Column(name = "total_haber")
    private BigDecimal totalHaber;

    @Column(name = "saldo_neto")
    private BigDecimal saldoNeto;

    @Column(name = "estado_saldo")
    private String estadoSaldo;
}