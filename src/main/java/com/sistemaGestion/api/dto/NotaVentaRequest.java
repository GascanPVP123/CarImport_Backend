package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class NotaVentaRequest {
    private Long cotizacionId;
    private Long clienteId;
    private String condicionPago;
    private String moneda;
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