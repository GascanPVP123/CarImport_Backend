package com.sistemaGestion.api.dto;

import com.sistemaGestion.api.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoResponseDTO(
        Long id,
        String serie,
        Integer correlativo,
        String numero,
        Long cotizacionId,
        String cotizacionNumero,
        Long clienteId,
        String clienteNombre,
        Long usuarioId,
        String usuarioNombre,
        LocalDate fechaEmision,
        String condicionPago,
        String moneda,
        BigDecimal total,
        Pedido.EstadoPedido estado,
        String observaciones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PedidoResponseDTO from(Pedido p) {
        var cot = p.getCotizacion();
        var cli = p.getCliente();
        var usr = p.getUsuario();
        return new PedidoResponseDTO(
                p.getId(),
                p.getSerie(),
                p.getCorrelativo(),
                p.getNumero(),
                cot != null ? cot.getId() : null,
                cot != null ? cot.getNumero() : null,
                cli != null ? cli.getId() : null,
                cli != null ? cli.getNombre() : null,
                usr != null ? usr.getId() : null,
                usr != null ? usr.getUsername() : null,
                p.getFechaEmision(),
                p.getCondicionPago(),
                p.getMoneda(),
                p.getTotal(),
                p.getEstado(),
                p.getObservaciones(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
