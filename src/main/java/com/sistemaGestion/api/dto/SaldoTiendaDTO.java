package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SaldoTiendaDTO {
    private Long tiendaId;
    private String tiendaNombre;
    private String telefono;
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal saldoNeto;
    private String estadoSaldo;
}