package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.PagoAsignacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface PagoAsignacionRepository extends JpaRepository<PagoAsignacion, Long> {

    List<PagoAsignacion> findByPagoId(Long pagoId);

    @EntityGraph(attributePaths = {"pago"})
    List<PagoAsignacion> findByCargoId(Long cargoId);

    /** Total ya aplicado a un cargo; saldo pendiente = cargo.monto - este valor. */
    @Query("SELECT COALESCE(SUM(a.montoAplicado), 0) FROM PagoAsignacion a WHERE a.cargo.id = :cargoId")
    BigDecimal sumMontoAplicadoByCargo(@Param("cargoId") Long cargoId);

    /** Total aplicado por cargo, en una sola consulta para varios cargos. */
    @Query("""
            SELECT a.cargo.id AS cargoId, SUM(a.montoAplicado) AS montoAplicado
            FROM PagoAsignacion a
            WHERE a.cargo.id IN :cargoIds
            GROUP BY a.cargo.id
            """)
    List<MontoAplicadoView> sumMontoAplicadoByCargos(@Param("cargoIds") Collection<Long> cargoIds);

    interface MontoAplicadoView {
        Long getCargoId();
        BigDecimal getMontoAplicado();
    }
}
