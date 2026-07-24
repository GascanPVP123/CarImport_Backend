package com.sistemaGestion.api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DevolucionConsignacionRequest {
    private LocalDate fechaDevolucion;
    private String motivo;
    private List<DetalleDevolucionDTO> detalles;

    @Data
    public static class DetalleDevolucionDTO {
        private Long detalleConsignacionId;
        private Integer cantidad;
    }
}