package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.CuentaCorriente;
import com.sistemaGestion.api.model.VistaSaldoTienda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaCorrienteRepository extends JpaRepository<CuentaCorriente, Long> {

    // Los @EntityGraph cargan las relaciones que lee MovimientoResponseDTO (evita N+1).

    // ===== Por tienda =====

    @EntityGraph(attributePaths = {"tienda", "cliente", "consignacion", "usuario"})
    List<CuentaCorriente> findByTiendaIdOrderByFechaDesc(Long tiendaId);

    List<CuentaCorriente> findByTiendaIdAndTipo(Long tiendaId, CuentaCorriente.Tipo tipo);

    // ===== Por cliente =====

    @EntityGraph(attributePaths = {"tienda", "cliente", "consignacion", "usuario"})
    List<CuentaCorriente> findByClienteIdOrderByFechaDesc(Long clienteId);

    /** Cargos (DEBITO) del cliente que aún no están pagados por completo, del más antiguo al más nuevo. */
    @Query("""
            SELECT c FROM CuentaCorriente c
            WHERE c.cliente.id = :clienteId
              AND c.tipo = com.sistemaGestion.api.model.CuentaCorriente.Tipo.DEBITO
              AND (c.estadoDocumento IS NULL OR c.estadoDocumento <> 'Pagado')
            ORDER BY COALESCE(c.fechaVencimiento, c.fecha) ASC, c.id ASC
            """)
    List<CuentaCorriente> findCargosPendientesByCliente(@Param("clienteId") Long clienteId);

    /**
     * Igual que findCargosPendientesByCliente pero bloqueando las filas (SELECT ... FOR UPDATE)
     * para que dos pagos concurrentes no se apliquen sobre el mismo saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM CuentaCorriente c
            WHERE c.cliente.id = :clienteId
              AND c.tipo = com.sistemaGestion.api.model.CuentaCorriente.Tipo.DEBITO
              AND (c.estadoDocumento IS NULL OR c.estadoDocumento <> 'Pagado')
            ORDER BY COALESCE(c.fechaVencimiento, c.fecha) ASC, c.id ASC
            """)
    List<CuentaCorriente> lockCargosPendientesByCliente(@Param("clienteId") Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaCorriente c WHERE c.id IN :ids ORDER BY c.id")
    List<CuentaCorriente> lockByIds(@Param("ids") Collection<Long> ids);

    Optional<CuentaCorriente> findFirstByOrigenAndReferencia(CuentaCorriente.Origen origen, String referencia);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN c.tipo = com.sistemaGestion.api.model.CuentaCorriente.Tipo.DEBITO
                                     THEN c.monto ELSE -c.monto END), 0)
            FROM CuentaCorriente c
            WHERE c.cliente.id = :clienteId
            """)
    BigDecimal calcularSaldoCliente(@Param("clienteId") Long clienteId);

    // ===== Por documento origen =====

    List<CuentaCorriente> findByConsignacionId(Long consignacionId);

    List<CuentaCorriente> findByNotaVentaId(Long notaVentaId);

    List<CuentaCorriente> findByPedidoId(Long pedidoId);

    List<CuentaCorriente> findByOrigen(CuentaCorriente.Origen origen);

    @Query("SELECT c FROM CuentaCorriente c WHERE c.fecha BETWEEN :desde AND :hasta ORDER BY c.fecha DESC")
    List<CuentaCorriente> findByRangoFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    // ===== Vista de saldos (vista_saldos_tiendas) =====

    @Query(value = "SELECT * FROM vista_saldos_tiendas", nativeQuery = true)
    List<VistaSaldoTienda> findSaldosTiendas();

    @Query(value = "SELECT * FROM vista_saldos_tiendas WHERE tienda_id = :tiendaId", nativeQuery = true)
    VistaSaldoTienda findSaldoByTiendaId(@Param("tiendaId") Long tiendaId);

    /** Totales globales de la vista, como proyección tipada en lugar de Object[]. */
    @Query(value = """
            SELECT COALESCE(SUM(total_debe), 0)  AS totalDebe,
                   COALESCE(SUM(total_haber), 0) AS totalHaber,
                   COALESCE(SUM(saldo_neto), 0)  AS saldoNeto,
                   COUNT(*)                      AS totalTiendas
            FROM vista_saldos_tiendas
            """, nativeQuery = true)
    ResumenCuentasView findResumenGeneral();

    interface ResumenCuentasView {
        BigDecimal getTotalDebe();
        BigDecimal getTotalHaber();
        BigDecimal getSaldoNeto();
        Long getTotalTiendas();
    }
}
