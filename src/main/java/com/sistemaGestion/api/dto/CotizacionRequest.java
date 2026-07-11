package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CotizacionRequest {
    private Long clienteId;
    private LocalDate fechaVencimiento;
    private String condicionPago;
    private String moneda;
    private String observaciones;
    private List<DetalleRequest> detalles;

    @Data
    public static class DetalleRequest {
        private Long productoId;
        private Integer cantidad;
        private String unidad;
        private BigDecimal precioUnitario;
        private BigDecimal descuento;
    }
}