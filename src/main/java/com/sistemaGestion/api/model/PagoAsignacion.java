package com.sistemaGestion.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Aplicación de (parte de) un pago a un cargo de cuenta corriente (tabla pagos_asignaciones).
 */
@Getter
@Setter
@Entity
@Table(name = "pagos_asignaciones")
public class PagoAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private PagoRecibo pago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_corriente_cargo_id", nullable = false)
    private CuentaCorriente cargo;

    @Column(name = "monto_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAplicado;
}
