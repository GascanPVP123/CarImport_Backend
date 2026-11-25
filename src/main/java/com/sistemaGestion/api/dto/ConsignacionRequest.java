package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ConsignacionRequest {
    private Long tiendaId;
    private LocalDate fechaEnvio;
    private String observaciones;
    private List<DetalleConsignacionDTO> detalles;

    @Data
    public static class DetalleConsignacionDTO {
        private Long productoId;
        private Integer cantidad;
        private BigDecimal precioUnitario;
    }
}