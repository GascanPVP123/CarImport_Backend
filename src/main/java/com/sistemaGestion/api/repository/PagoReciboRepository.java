package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.PagoRecibo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoReciboRepository extends JpaRepository<PagoRecibo, Long> {

    @EntityGraph(attributePaths = {"cliente", "asignaciones", "asignaciones.cargo"})
    Optional<PagoRecibo> findWithAsignacionesById(Long id);

    @EntityGraph(attributePaths = {"cliente"})
    List<PagoRecibo> findAllByOrderByFechaAbonoDesc();

    @EntityGraph(attributePaths = {"cliente"})
    List<PagoRecibo> findByClienteIdOrderByFechaAbonoDesc(Long clienteId);

    @EntityGraph(attributePaths = {"cliente"})
    List<PagoRecibo> findByFechaAbonoBetweenOrderByFechaAbonoDesc(LocalDateTime desde, LocalDateTime hasta);

    /** Evita registrar dos veces el mismo voucher bancario. */
    boolean existsByNumeroOperacionBancariaAndCuentaRecaudadora(String numeroOperacionBancaria,
                                                                 String cuentaRecaudadora);

    @Query("SELECT COALESCE(SUM(p.importeTotal), 0) FROM PagoRecibo p WHERE p.cliente.id = :clienteId")
    BigDecimal sumImporteByCliente(@Param("clienteId") Long clienteId);
}
