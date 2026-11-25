package com.sistemaGestion.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de un voucher/pago de cliente y su distribución entre cargos de cuenta corriente.
 * La suma de asignaciones no debe superar importeTotal.
 */
@Data
public class PagoReciboRequest {
    private Long clienteId;
    private LocalDateTime fechaAbono;
    private String medioPago;
    private String numeroOperacionBancaria;
    private String cuentaRecaudadora;
    private BigDecimal importeTotal;
    private String comprobanteUrl;
    private List<Asignacion> asignaciones = new ArrayList<>();

    @Data
    public static class Asignacion {
        private Long cuentaCorrienteCargoId;
        private BigDecimal montoAplicado;
    }
}
