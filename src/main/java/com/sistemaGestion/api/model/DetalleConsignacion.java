package com.sistemaGestion.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "detalles_consignacion")
public class DetalleConsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignacion_id", nullable = false)
    @JsonIgnoreProperties({"detalles", "tienda", "usuario"})
    private Consignacion consignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto producto;

    @Column(name = "cantidad_enviada", nullable = false)
    private Integer cantidadEnviada = 0;

    @Column(name = "cantidad_vendida", nullable = false)
    private Integer cantidadVendida = 0;

    @Column(name = "cantidad_devuelta", nullable = false)
    private Integer cantidadDevuelta = 0;

    @Column(name = "precio_unitario", precision = 10, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean devuelto = false;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Transient
    public Integer getCantidadPendiente() {
        return cantidadEnviada - cantidadVendida - cantidadDevuelta;
    }
}