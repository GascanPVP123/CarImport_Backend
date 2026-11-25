package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Alta manual de un movimiento de cuenta corriente. Se requiere tiendaId o clienteId. */
@Data
public class MovimientoRequest {
    private Long tiendaId;
    private Long clienteId;
    private String tipo;       // DEBITO o CREDITO
    private String origen;     // VENTA_DIRECTA, COMPRA, PAGO_RECIBIDO, etc.
    private String concepto;
    private BigDecimal monto;
    private LocalDate fecha;
    private LocalDate fechaVencimiento;
    private String referencia;
    private String documentoRef;
    private Long notaVentaId;
    private Long pedidoId;
    private String observaciones;
}
