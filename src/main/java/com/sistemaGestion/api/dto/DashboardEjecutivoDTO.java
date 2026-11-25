package com.sistemaGestion.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardEjecutivoDTO(
        KpiDTO kpis,
        List<TendenciaMensualDTO> tendenciaIngresos,
        DistribucionCarteraGeneralDTO distribucionCartera,
        List<OperacionRecienteDTO> ultimasOperaciones
) {

    public record KpiDTO(
            BigDecimal ventasDelMes,
            BigDecimal porcentajeVariacionVentas,
            BigDecimal utilidadBrutaEstimada,
            BigDecimal margenPorcentaje,
            BigDecimal cuentasPorCobrar,
            Long clientesConSaldoPendiente,
            BigDecimal valorEnConsignacion,
            Long tiendasAliadasActivas
    ) {}

    public record TendenciaMensualDTO(
            String mes,
            Integer numeroMes,
            BigDecimal ventas,
            BigDecimal cobranzas
    ) {}

    public record DistribucionCarteraGeneralDTO(
            BigDecimal totalMes,
            List<CarteraItemDTO> canales
    ) {}

    public record CarteraItemDTO(
            String canal,
            BigDecimal montoTotal,
            BigDecimal porcentaje,
            String colorHex
    ) {}

    public record OperacionRecienteDTO(
            Long id,
            String nroComprobante,
            String tipoOperacion,
            String clienteProveedor,
            String inicialesCliente,
            LocalDateTime fechaHora,
            BigDecimal monto,
            String estado,
            String icono                        
    ) {}
}