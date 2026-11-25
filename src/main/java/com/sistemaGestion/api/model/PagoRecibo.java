package com.sistemaGestion.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pago / voucher recibido de un cliente (tabla pagos_recibos).
 * Su importe se distribuye entre cargos de cuentas_corrientes mediante PagoAsignacion.
 */
@Getter
@Setter
@Entity
@Table(name = "pagos_recibos")
public class PagoRecibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "fecha_abono", nullable = false)
    private LocalDateTime fechaAbono;

    @Column(name = "medio_pago", length = 50)
    private String medioPago;

    @Column(name = "numero_operacion_bancaria", length = 100)
    private String numeroOperacionBancaria;

    @Column(name = "cuenta_recaudadora", length = 100)
    private String cuentaRecaudadora;

    @Column(name = "importe_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @Column(name = "comprobante_url", columnDefinition = "TEXT")
    private String comprobanteUrl;

    /** ON DELETE CASCADE en BD; aquí también en cascada para persistir pago + asignaciones juntos. */
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoAsignacion> asignaciones = new ArrayList<>();

    public void agregarAsignacion(PagoAsignacion asignacion) {
        asignacion.setPago(this);
        asignaciones.add(asignacion);
    }

    public BigDecimal getMontoAsignado() {
        return asignaciones.stream()
                .map(PagoAsignacion::getMontoAplicado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    protected void onCreate() {
        if (fechaAbono == null) fechaAbono = LocalDateTime.now();
    }
}
