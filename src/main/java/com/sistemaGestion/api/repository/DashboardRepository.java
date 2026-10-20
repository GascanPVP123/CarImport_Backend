package com.sistemaGestion.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * 1. KPIs Superiores consultando tus tablas reales en plural:
     *    cuentas_corrientes, consignaciones, productos
     */
    public Tuple obtenerMetricasKpi(int anio, int mes) {
        String sql = """
            WITH ventas_actuales AS (
                SELECT COALESCE(SUM(monto), 0) AS total_ventas_mes
                FROM cuentas_corrientes
                WHERE tipo = 'DEBITO'
                  AND EXTRACT(YEAR FROM fecha) = :anio
                  AND EXTRACT(MONTH FROM fecha) = :mes
            ),
            ventas_anteriores AS (
                SELECT COALESCE(SUM(monto), 0) AS total_ventas_ant
                FROM cuentas_corrientes
                WHERE tipo = 'DEBITO'
                  AND EXTRACT(YEAR FROM fecha) = :anio
                  AND EXTRACT(MONTH FROM fecha) = (:mes - 1)
            ),
            saldos_tiendas AS (
                SELECT 
                    tienda_id,
                    SUM(CASE WHEN tipo = 'DEBITO' THEN monto ELSE -monto END) AS saldo
                FROM cuentas_corrientes
                GROUP BY tienda_id
                HAVING SUM(CASE WHEN tipo = 'DEBITO' THEN monto ELSE -monto END) > 0
            ),
            consignaciones_activas AS (
                SELECT 
                    COALESCE(SUM(valor_total - COALESCE(valor_vendido, 0)), 0) AS valor_activo,
                    COUNT(DISTINCT tienda_id) AS tiendas_en_plaza
                FROM consignaciones
                WHERE estado IN ('ENVIADA', 'PARCIAL', 'ACTIVA', 'PENDIENTE')
            ),
            margen_catalogo AS (
                SELECT COALESCE(AVG((precio_menor - precio_compra) / NULLIF(precio_menor, 0)), 0.283) AS ratio_margen
                FROM productos
                WHERE precio_menor > 0 AND precio_compra > 0
            )
            SELECT 
                va.total_ventas_mes AS ventasMes,
                vant.total_ventas_ant AS ventasMesAnterior,
                mc.ratio_margen AS ratioMargen,
                COALESCE((SELECT SUM(saldo) FROM saldos_tiendas), 0) AS cuentasPorCobrar,
                COALESCE((SELECT COUNT(*) FROM saldos_tiendas), 0) AS clientesDeudores,
                ca.valor_activo AS valorConsignacion,
                ca.tiendas_en_plaza AS tiendasActivas
            FROM ventas_actuales va
            CROSS JOIN ventas_anteriores vant
            CROSS JOIN margen_catalogo mc
            CROSS JOIN consignaciones_activas ca
            """;

        return (Tuple) em.createNativeQuery(sql, Tuple.class)
                .setParameter("anio", anio)
                .setParameter("mes", mes)
                .getSingleResult();
    }

    /**
     * 2. Gráfico de Tendencia (Ene - Oct 2026) sobre cuentas_corrientes
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> obtenerTendenciaMensual(int anio) {
        String sql = """
            WITH meses AS (
                SELECT generate_series(1, 10) AS num_mes
            ),
            movimientos_mes AS (
                SELECT 
                    EXTRACT(MONTH FROM fecha)::int AS num_mes,
                    SUM(CASE WHEN tipo = 'DEBITO' THEN monto ELSE 0 END) AS ventas,
                    SUM(CASE WHEN tipo = 'CREDITO' THEN monto ELSE 0 END) AS cobranzas
                FROM cuentas_corrientes
                WHERE EXTRACT(YEAR FROM fecha) = :anio
                GROUP BY EXTRACT(MONTH FROM fecha)
            )
            SELECT 
                m.num_mes AS numeroMes,
                CASE m.num_mes
                    WHEN 1 THEN 'Ene' WHEN 2 THEN 'Feb' WHEN 3 THEN 'Mar'
                    WHEN 4 THEN 'Abr' WHEN 5 THEN 'May' WHEN 6 THEN 'Jun'
                    WHEN 7 THEN 'Jul' WHEN 8 THEN 'Ago' WHEN 9 THEN 'Set'
                    WHEN 10 THEN 'Oct'
                END AS nombreMes,
                COALESCE(mm.ventas, 0) AS ventas,
                COALESCE(mm.cobranzas, 0) AS cobranzas
            FROM meses m
            LEFT JOIN movimientos_mes mm ON m.num_mes = mm.num_mes
            ORDER BY m.num_mes ASC
            """;

        return em.createNativeQuery(sql, Tuple.class)
                .setParameter("anio", anio)
                .getResultList();
    }

    /**
     * 3. Últimas 10 Operaciones unificando consignaciones, cotizaciones y cuentas_corrientes
     */
    @SuppressWarnings("unchecked")
    public List<Tuple> obtenerUltimasOperaciones() {
        String sql = """
            (
                SELECT 
                    c.id AS id,
                    c.numero_consignacion AS nroComprobante,
                    'Despacho Consignación' AS tipoOperacion,
                    t.nombre AS clienteProveedor,
                    c.fecha_envio::timestamp AS fechaHora,
                    c.valor_total AS monto,
                    CASE 
                        WHEN c.estado = 'ENVIADA' THEN 'En Tránsito'
                        WHEN c.estado = 'LIQUIDADA' THEN 'Cobrado'
                        ELSE c.estado 
                    END AS estado,
                    'truck' AS icono
                FROM consignaciones c
                JOIN tiendas_aliadas t ON c.tienda_id = t.id
            )
            UNION ALL
            (
                SELECT 
                    cc.id AS id,
                    CONCAT(CASE WHEN cc.tipo = 'CREDITO' THEN 'PAG-000' ELSE 'PED-000' END, cc.id) AS nroComprobante,
                    CASE WHEN cc.tipo = 'CREDITO' THEN 'Cobranza Letra' ELSE 'Venta Mayorista' END AS tipoOperacion,
                    t.nombre AS clienteProveedor,
                    cc.fecha::timestamp AS fechaHora,
                    cc.monto AS monto,
                    CASE WHEN cc.tipo = 'CREDITO' THEN 'Acreditado' ELSE 'Cobrado' END AS estado,
                    CASE WHEN cc.tipo = 'CREDITO' THEN 'credit-card' ELSE 'shopping-bag' END AS icono
                FROM cuentas_corrientes cc
                JOIN tiendas_aliadas t ON cc.tienda_id = t.id
            )
            ORDER BY fechaHora DESC
            LIMIT 10
            """;

        return em.createNativeQuery(sql, Tuple.class).getResultList();
    }
}