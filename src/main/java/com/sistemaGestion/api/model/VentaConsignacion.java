package com.sistemaGestion.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ventas_consignacion")
public class VentaConsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignacion_id", nullable = false)
    @JsonIgnoreProperties({"detalles", "tienda", "usuario"})
    private Consignacion consignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_consignacion_id", nullable = false)
    @JsonIgnoreProperties({"consignacion"})
    private DetalleConsignacion detalleConsignacion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    @Column(name = "comision_porcentaje", precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje = BigDecimal.ZERO;

    @Column(name = "comision_monto", precision = 10, scale = 2)
    private BigDecimal comisionMonto = BigDecimal.ZERO;

    @Column(name = "monto_a_pagar", precision = 10, scale = 2)
    private BigDecimal montoAPagar = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Usuario usuario;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}