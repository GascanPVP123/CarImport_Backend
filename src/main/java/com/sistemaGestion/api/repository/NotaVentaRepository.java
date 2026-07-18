package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.NotaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotaVentaRepository extends JpaRepository<NotaVenta, Long> {
}