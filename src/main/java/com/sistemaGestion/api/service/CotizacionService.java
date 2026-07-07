package com.sistemaGestion.api.service;

import com.sistemaGestion.api.model.Cotizacion;
import com.sistemaGestion.api.model.DetalleCotizacion;
import com.sistemaGestion.api.model.Producto;
import com.sistemaGestion.api.repository.CotizacionRepository;
import com.sistemaGestion.api.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CotizacionService {

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Cotizacion generarCotizacion(Cotizacion cotizacion) {
        double totalGeneral = 0.0;

        if (cotizacion.getDetalles() != null) {
            for (DetalleCotizacion detalle : cotizacion.getDetalles()) {

                // 1. Amarrar la relación bidireccional
                detalle.setCotizacion(cotizacion);

                // 2. Buscar el producto en la DB
                Producto prod = productoRepository.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + detalle.getProducto().getId()));

                // ======================================================================
                // 🔥 CONTROL DE STOCK AUTOMÁTICO
                // ======================================================================
                // Validamos si hay suficiente mercancía en el inventario
                if (prod.getStock() < detalle.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente para el producto '" + prod.getNombre() +
                            "'. Stock actual: " + prod.getStock() + ", solicitado: " + detalle.getCantidad());
                }

                // Restamos la cantidad solicitada del stock actual
                prod.setStock(prod.getStock() - detalle.getCantidad());

                // Guardamos el producto con su nuevo stock actualizado
                productoRepository.save(prod);
                // ======================================================================

                detalle.setProducto(prod);

                double precioVenta = prod.getPrecioVenta().doubleValue();
                detalle.setPrecioCotizado(precioVenta);

                totalGeneral += precioVenta * detalle.getCantidad();
            }
        }

        cotizacion.setTotal(totalGeneral);
        cotizacion.setFecha(LocalDateTime.now());

        // Guardamos la cotización y sus detalles
        Cotizacion guardada = cotizacionRepository.save(cotizacion);

        // Sincronizamos y refrescamos para que Postman devuelva el JSON limpio
        entityManager.flush();
        entityManager.refresh(guardada);

        return guardada;
    }

    public List<Cotizacion> listarTodas() {
        return cotizacionRepository.findAll();
    }
}