package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.Consignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConsignacionRepository extends JpaRepository<Consignacion, Long> {

    List<Consignacion> findByTiendaId(Long tiendaId);

    List<Consignacion> findByEstado(Consignacion.EstadoConsignacion estado);

    @Query("SELECT c FROM Consignacion c WHERE c.estado IN ('ENVIADA', 'PARCIAL')")
    List<Consignacion> findConsignacionesActivas();

    @Query("SELECT COALESCE(MAX(c.numeroConsignacion), 'CONS-000000') FROM Consignacion c")
    String findUltimoNumero();

    @Query("SELECT c FROM Consignacion c JOIN FETCH c.tienda JOIN FETCH c.detalles WHERE c.id = :id")
    Consignacion findByIdWithDetalles(@Param("id") Long id);
}