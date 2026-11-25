package com.sistemaGestion.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pedido de venta (tabla pedidos).
 *
 * IMPORTANTE: el DDL de "pedidos" no está en docs/database.md (archivo truncado).
 * Las columnas se infirieron siguiendo la convención de cotizaciones / notas_venta.
 * Verificar contra la BD real antes de usar en producción
 * (activar spring.jpa.hibernate.ddl-auto=validate lo detecta al arrancar).
 * Confirmado por el doc: id, updated_at (trigger trg_pedidos_updated_at) y FK desde cuentas_corrientes.
 */
@Getter
@Setter
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 4)
    private String serie = "PED";

    @Column(nullable = false)
    private Integer correlativo;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "condicion_pago", length = 20)
    private String condicionPago = "CONTADO";

    @Column(length = 5)
    private String moneda = "SOLES";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum EstadoPedido {
        PENDIENTE, CONFIRMADO, DESPACHADO, ENTREGADO, ANULADO
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (fechaEmision == null) fechaEmision = LocalDate.now();
        if (total == null) total = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
