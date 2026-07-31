package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.CuentaCorriente;
import com.sistemaGestion.api.model.VistaSaldoTienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CuentaCorrienteRepository extends JpaRepository<CuentaCorriente, Long> {

    // Movimientos de una tienda ordenados por fecha
    List<CuentaCorriente> findByTiendaIdOrderByFechaDesc(Long tiendaId);

    // Movimientos de una consignación
    List<CuentaCorriente> findByConsignacionId(Long consignacionId);

    // Movimientos por tipo
    List<CuentaCorriente> findByTiendaIdAndTipo(Long tiendaId, CuentaCorriente.Tipo tipo);

    // Movimientos por origen
    List<CuentaCorriente> findByOrigen(CuentaCorriente.Origen origen);

    // Movimientos por rango de fechas
    @Query("SELECT c FROM CuentaCorriente c WHERE c.fecha BETWEEN :desde AND :hasta ORDER BY c.fecha DESC")
    List<CuentaCorriente> findByRangoFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    // Vista de saldos
    @Query(value = "SELECT * FROM vista_saldos_tiendas", nativeQuery = true)
    List<VistaSaldoTienda> findSaldosTiendas();

    // Saldo de una tienda específica
    @Query(value = "SELECT * FROM vista_saldos_tiendas WHERE tienda_id = :tiendaId", nativeQuery = true)
    VistaSaldoTienda findSaldoByTiendaId(@Param("tiendaId") Long tiendaId);

    // Resumen general
    @Query(value = "SELECT " +
            "COALESCE(SUM(total_debe), 0) as totalDebe, " +
            "COALESCE(SUM(total_haber), 0) as totalHaber, " +
            "COALESCE(SUM(saldo_neto), 0) as saldoNeto, " +
            "COUNT(*) as totalTiendas " +
            "FROM vista_saldos_tiendas", nativeQuery = true)
    Object findResumenGeneral();
}