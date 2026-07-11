package com.sistemaGestion.api.repository;

import com.sistemaGestion.api.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByStockLessThanEqual(Integer stockMinimo);
    List<Producto> findByImportadoraId(Long importadoraId);
    List<Producto> findByProveedorId(Long proveedorId);
}