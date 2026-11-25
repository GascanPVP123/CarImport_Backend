package com.sistemaGestion.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Movimiento del libro de cuentas corrientes (tabla cuentas_corrientes).
 * Un movimiento pertenece a una tienda aliada o a un cliente.
 * Se expone siempre mediante DTOs: no serializar la entidad directamente.
 */
@Getter
@Setter
@Entity
@Table(name = "cuentas_corrientes")
public class CuentaCorriente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id")
    private TiendaAliada tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(length = 7, nullable = false)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 23, nullable = false)
    private Origen origen;

    @Column(nullable = false, length = 200)
    private String concepto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignacion_id")
    private Consignacion consignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_venta_id")
    private NotaVenta notaVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(name = "monto_cargo", precision = 10, scale = 2)
    private BigDecimal montoCargo = BigDecimal.ZERO;

    @Column(name = "monto_abono", precision = 10, scale = 2)
    private BigDecimal montoAbono = BigDecimal.ZERO;

    @Column(name = "saldo_acumulado", precision = 10, scale = 2)
    private BigDecimal saldoAcumulado = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(length = 100)
    private String referencia;

    @Column(name = "documento_ref", length = 100)
    private String documentoRef;

    @Column(name = "estado_documento", length = 50)
    private String estadoDocumento = EstadoDocumento.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Tipo {
        DEBITO,   // La tienda / cliente te debe (cargo)
        CREDITO   // Tú le debes, o abono recibido
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

    /** Valores usados en estado_documento (columna libre VARCHAR(50), default 'Pendiente'). */
    public static final class EstadoDocumento {
        public static final String PENDIENTE = "Pendiente";
        public static final String PARCIAL = "Parcial";
        public static final String PAGADO = "Pagado";

        private EstadoDocumento() {
        }
    }

    @PrePersist
    protected void onCreate() {
        if (tienda == null && cliente == null) {
            throw new IllegalStateException("El movimiento debe pertenecer a una tienda o a un cliente");
        }
        if (monto == null) monto = BigDecimal.ZERO;
        // Si no se informan cargo/abono explícitos, se derivan del tipo
        if (isZero(montoCargo) && isZero(montoAbono)) {
            montoCargo = tipo == Tipo.DEBITO ? monto : BigDecimal.ZERO;
            montoAbono = tipo == Tipo.CREDITO ? monto : BigDecimal.ZERO;
        }
        if (saldoAcumulado == null) saldoAcumulado = BigDecimal.ZERO;
        if (estadoDocumento == null) estadoDocumento = EstadoDocumento.PENDIENTE;
        createdAt = LocalDateTime.now();
    }

    private static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }
}
