package com.sistemaGestion.api.model;

import java.math.BigDecimal;

public interface VistaSaldoTienda {
    Long getTiendaId();
    String getTiendaNombre();
    String getTelefono();
    BigDecimal getTotalDebe();
    BigDecimal getTotalHaber();
    BigDecimal getSaldoNeto();
    String getEstadoSaldo();
}