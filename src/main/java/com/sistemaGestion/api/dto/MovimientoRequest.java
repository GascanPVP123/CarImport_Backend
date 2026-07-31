package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovimientoRequest {
    private Long tiendaId;
    private String tipo;       // DEBITO o CREDITO
    private String origen;     // VENTA_DIRECTA, COMPRA, PAGO_RECIBIDO, etc.
    private String concepto;
    private BigDecimal monto;
    private LocalDate fecha;
    private String referencia;
    private String observaciones;
}