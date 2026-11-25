package com.sistemaGestion.api.dto;

import com.sistemaGestion.api.model.PagoAsignacion;
import com.sistemaGestion.api.model.PagoRecibo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pago recibido con sus asignaciones. Usar con PagoReciboRepository.findWithAsignacionesById
 * (o dentro de la transacción) para tener cargadas las asignaciones y sus cargos.
 */
public record PagoReciboResponseDTO(
        Long id,
        Long clienteId,
        String clienteNombre,
        LocalDateTime fechaAbono,
        String medioPago,
        String numeroOperacionBancaria,
        String cuentaRecaudadora,
        BigDecimal importeTotal,
        BigDecimal montoAsignado,
        BigDecimal montoSinAsignar,
        String comprobanteUrl,
        List<AsignacionDTO> asignaciones
) {

    public record AsignacionDTO(
            Long id,
            Long cuentaCorrienteCargoId,
            String concepto,
            String documentoRef,
            LocalDate fechaCargo,
            BigDecimal montoCargo,
            BigDecimal montoAplicado
    ) {
        public static AsignacionDTO from(PagoAsignacion a) {
            var cargo = a.getCargo();
            return new AsignacionDTO(
                    a.getId(),
                    cargo.getId(),
                    cargo.getConcepto(),
                    cargo.getDocumentoRef(),
                    cargo.getFecha(),
                    cargo.getMonto(),
                    a.getMontoAplicado()
            );
        }
    }

    public static PagoReciboResponseDTO from(PagoRecibo p) {
        BigDecimal asignado = p.getMontoAsignado();
        return new PagoReciboResponseDTO(
                p.getId(),
                p.getCliente().getId(),
                p.getCliente().getNombre(),
                p.getFechaAbono(),
                p.getMedioPago(),
                p.getNumeroOperacionBancaria(),
                p.getCuentaRecaudadora(),
                p.getImporteTotal(),
                asignado,
                p.getImporteTotal().subtract(asignado),
                p.getComprobanteUrl(),
                p.getAsignaciones().stream().map(AsignacionDTO::from).toList()
        );
    }

    /** Versión resumida para listados (sin cargar asignaciones). */
    public static PagoReciboResponseDTO resumen(PagoRecibo p) {
        return new PagoReciboResponseDTO(
                p.getId(),
                p.getCliente().getId(),
                p.getCliente().getNombre(),
                p.getFechaAbono(),
                p.getMedioPago(),
                p.getNumeroOperacionBancaria(),
                p.getCuentaRecaudadora(),
                p.getImporteTotal(),
                null,
                null,
                p.getComprobanteUrl(),
                List.of()
        );
    }
}
