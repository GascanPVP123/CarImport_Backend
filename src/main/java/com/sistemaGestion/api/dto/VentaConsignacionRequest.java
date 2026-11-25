package com.sistemaGestion.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class VentaConsignacionRequest {
    private LocalDate fechaVenta;
    private BigDecimal comisionPorcentaje;
    private String observaciones;
    private List<DetalleVentaDTO> detalles;

    @Data
    public static class DetalleVentaDTO {
        private Long detalleConsignacionId;
        private Integer cantidad;
        private BigDecimal precioUnitario;
    }
}