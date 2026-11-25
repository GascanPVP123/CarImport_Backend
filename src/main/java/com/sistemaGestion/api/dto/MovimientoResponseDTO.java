package com.sistemaGestion.api.dto;

import com.sistemaGestion.api.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Movimiento de cuenta corriente expuesto por la API.
 * Mantiene la forma que ya consume el frontend (tienda / consignacion / usuario anidados)
 * sin exponer la entidad JPA (ni el hash de contraseña del usuario).
 * Debe construirse dentro de la transacción o con las relaciones ya cargadas (@EntityGraph).
 */
public record MovimientoResponseDTO(
        Long id,
        TiendaRef tienda,
        ClienteRef cliente,
        CuentaCorriente.Tipo tipo,
        CuentaCorriente.Origen origen,
        String concepto,
        ConsignacionRef consignacion,
        Long notaVentaId,
        Long pedidoId,
        BigDecimal monto,
        BigDecimal montoCargo,
        BigDecimal montoAbono,
        BigDecimal saldoAcumulado,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        String referencia,
        String documentoRef,
        String estadoDocumento,
        String observaciones,
        UsuarioRef usuario,
        LocalDateTime createdAt
) {

    public record TiendaRef(Long id, String nombre, String ruc, String telefono) {
        static TiendaRef of(TiendaAliada t) {
            return t == null ? null : new TiendaRef(t.getId(), t.getNombre(), t.getRuc(), t.getTelefono());
        }
    }

    public record ClienteRef(Long id, String nombre, String documento) {
        static ClienteRef of(Cliente c) {
            return c == null ? null : new ClienteRef(c.getId(), c.getNombre(), c.getDocumento());
        }
    }

    public record ConsignacionRef(Long id, String numeroConsignacion) {
        static ConsignacionRef of(Consignacion c) {
            return c == null ? null : new ConsignacionRef(c.getId(), c.getNumeroConsignacion());
        }
    }

    public record UsuarioRef(Long id, String username) {
        static UsuarioRef of(Usuario u) {
            return u == null ? null : new UsuarioRef(u.getId(), u.getUsername());
        }
    }

    public static MovimientoResponseDTO from(CuentaCorriente cc) {
        return new MovimientoResponseDTO(
                cc.getId(),
                TiendaRef.of(cc.getTienda()),
                ClienteRef.of(cc.getCliente()),
                cc.getTipo(),
                cc.getOrigen(),
                cc.getConcepto(),
                ConsignacionRef.of(cc.getConsignacion()),
                // getId() sobre un proxy lazy no dispara consulta
                cc.getNotaVenta() != null ? cc.getNotaVenta().getId() : null,
                cc.getPedido() != null ? cc.getPedido().getId() : null,
                cc.getMonto(),
                cc.getMontoCargo(),
                cc.getMontoAbono(),
                cc.getSaldoAcumulado(),
                cc.getFecha(),
                cc.getFechaVencimiento(),
                cc.getReferencia(),
                cc.getDocumentoRef(),
                cc.getEstadoDocumento(),
                cc.getObservaciones(),
                UsuarioRef.of(cc.getUsuario()),
                cc.getCreatedAt()
        );
    }
}
