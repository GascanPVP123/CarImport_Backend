package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.TiendaAliada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TiendaAliadaRepository extends JpaRepository<TiendaAliada, Long> {
    List<TiendaAliada> findByActivoTrue();
    boolean existsByRuc(String ruc);
}