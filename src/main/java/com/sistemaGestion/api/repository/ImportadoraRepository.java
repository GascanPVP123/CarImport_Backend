package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.Importadora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportadoraRepository extends JpaRepository<Importadora, Long> {

    boolean existsByRuc(String ruc);

    /** RUC usado por otra importadora distinta de {@code id}. */
    boolean existsByRucAndIdNot(String ruc, Long id);
}
