package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.DevolucionConsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DevolucionConsignacionRepository extends JpaRepository<DevolucionConsignacion, Long> {

    List<DevolucionConsignacion> findByConsignacionId(Long consignacionId);
}