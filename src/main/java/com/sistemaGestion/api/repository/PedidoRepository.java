package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumero(String numero);

    boolean existsByCotizacionId(Long cotizacionId);

    Optional<Pedido> findByCotizacionId(Long cotizacionId);

    @EntityGraph(attributePaths = {"cliente", "usuario", "cotizacion"})
    List<Pedido> findByClienteIdOrderByFechaEmisionDesc(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "usuario", "cotizacion"})
    List<Pedido> findByEstadoOrderByFechaEmisionDesc(Pedido.EstadoPedido estado);

    @EntityGraph(attributePaths = {"cliente", "usuario", "cotizacion"})
    List<Pedido> findAllByOrderByFechaEmisionDesc();

    /** Siguiente correlativo disponible (evita usar count(), que se repite tras borrados). */
    @Query("SELECT COALESCE(MAX(p.correlativo), 0) + 1 FROM Pedido p")
    Integer findSiguienteCorrelativo();
}
