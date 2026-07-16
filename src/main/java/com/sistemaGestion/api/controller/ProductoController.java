package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.model.Producto;
import com.sistemaGestion.api.repository.ProductoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ProductoController {

    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Long id) {
        return productoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stock-bajo")
    public List<Producto> obtenerStockBajo() {
        return productoRepository.findByStockLessThanEqual(3);
    }

    @PostMapping
    public Producto crear(@RequestBody Producto producto) {
        return productoRepository.save(producto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @RequestBody Producto detalles) {
        return productoRepository.findById(id)
                .map(producto -> {
                    producto.setCodigoSku(detalles.getCodigoSku());
                    producto.setNombre(detalles.getNombre());
                    producto.setImagenUrl(detalles.getImagenUrl());
                    producto.setDescripcion(detalles.getDescripcion());
                    producto.setPrecioCompra(detalles.getPrecioCompra());
                    producto.setPrecioVenta(detalles.getPrecioVenta());
                    producto.setStock(detalles.getStock());
                    producto.setStockMinimo(detalles.getStockMinimo());
                    producto.setUnidadMedida(detalles.getUnidadMedida());
                    producto.setImportadora(detalles.getImportadora());
                    producto.setProveedor(detalles.getProveedor());
                    return ResponseEntity.ok(productoRepository.save(producto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            productoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar: " + e.getMessage());
        }
    }
}