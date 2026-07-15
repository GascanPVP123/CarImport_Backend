package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.Importadora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ImportadoraRepository extends JpaRepository<Importadora, Long> {
}
