package com.sistemaGestion.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cargo (DEBITO) de un cliente con su saldo aún por cobrar, para elegir a qué aplicar un pago. */
public record CargoPendienteDTO(
        Long id,
        String concepto,
        String documentoRef,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        BigDecimal monto,
        BigDecimal montoAplicado,
        BigDecimal saldoPendiente,
        String estadoDocumento
) {
}
