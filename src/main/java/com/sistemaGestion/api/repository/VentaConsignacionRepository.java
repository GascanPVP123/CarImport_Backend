package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.VentaConsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VentaConsignacionRepository extends JpaRepository<VentaConsignacion, Long> {

    List<VentaConsignacion> findByConsignacionId(Long consignacionId);
}