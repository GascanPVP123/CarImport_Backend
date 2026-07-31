package com.sistemaGestion.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cuentas_corrientes")
public class CuentaCorriente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TiendaAliada tienda;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private Origen origen;

    @Column(nullable = false, length = 200)
    private String concepto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignacion_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Consignacion consignacion;

    @Column(name = "nota_venta_id")
    private Long notaVentaId;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 100)
    private String referencia;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Tipo {
        DEBITO,   // La tienda te debe
        CREDITO   // Tú le debes a la tienda
    }

    public enum Origen {
        CONSIGNACION_VENTA,
        CONSIGNACION_COMISION,
        CONSIGNACION_DEVOLUCION,
        VENTA_DIRECTA,
        COMPRA,
        PAGO_RECIBIDO,
        GASTO_COMPARTIDO,
        AJUSTE,
        OTRO
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}