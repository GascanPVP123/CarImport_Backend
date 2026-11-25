package com.sistemaGestion.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record NotaVentaResponseDTO(
        Long id,
        String numeroDocumento, // Ej: NV-000001-2026
        LocalDateTime fechaEmision,
        String clienteNombre,
        String clienteDocumento, // RUC / DNI
        BigDecimal total,
        String estado, // EMITIDA, ANULADA
        List<DetalleDTO> detalles
) {
    public record DetalleDTO(
            Long productoId,
            String sku,
            String descripcion,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}
}