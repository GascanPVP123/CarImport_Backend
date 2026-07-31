package com.sistemaGestion.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ResumenCuentasDTO {
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal saldoNeto;
    private Long totalTiendas;
}