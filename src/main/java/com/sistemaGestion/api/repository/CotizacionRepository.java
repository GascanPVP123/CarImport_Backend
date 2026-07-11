package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    Optional<Cotizacion> findByNumero(String numero);
    List<Cotizacion> findByClienteId(Long clienteId);
    List<Cotizacion> findByEstado(String estado);
    long count();
}