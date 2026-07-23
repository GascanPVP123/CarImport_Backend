package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.model.Producto;
import com.sistemaGestion.api.repository.ProductoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ProductoController {

    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> obtenerTodos() {
        try {
            List<Producto> productos = productoRepository.findAll();
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return productoRepository.findById(id)
                    .map(producto -> ResponseEntity.ok((Object) producto))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Producto no encontrado con ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener el producto: " + e.getMessage()));
        }
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<Producto>> obtenerStockBajo() {
        try {
            List<Producto> productos = productoRepository.findByStockLessThanEqual(3);
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Producto producto) {
        try {
            // Validar campos obligatorios
            if (producto.getCodigoSku() == null || producto.getCodigoSku().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El código SKU es obligatorio"));
            }

            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El nombre del producto es obligatorio"));
            }

            // Verificar si el SKU ya existe
            if (productoRepository.existsByCodigoSku(producto.getCodigoSku())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El código SKU '" + producto.getCodigoSku() + "' ya existe"));
            }

            // Si tiene ID, verificar que no exista ya
            if (producto.getId() != null && productoRepository.existsById(producto.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El producto con ID " + producto.getId() + " ya existe. Use PUT para actualizar"));
            }

            Producto nuevoProducto = productoRepository.save(producto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProducto);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Violación de integridad: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear el producto: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Producto detalles) {
        try {
            // Validar que el producto existe
            if (!productoRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Producto no encontrado con ID: " + id));
            }

            // Validar campos obligatorios
            if (detalles.getCodigoSku() == null || detalles.getCodigoSku().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El código SKU es obligatorio"));
            }

            if (detalles.getNombre() == null || detalles.getNombre().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El nombre del producto es obligatorio"));
            }

            // Buscar el producto existente
            Producto producto = productoRepository.findById(id).orElse(null);
            if (producto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Producto no encontrado con ID: " + id));
            }

            // Actualizar campos
            producto.setCodigoSku(detalles.getCodigoSku().trim().toUpperCase());
            producto.setNombre(detalles.getNombre().trim());
            producto.setImagenUrl(detalles.getImagenUrl());
            producto.setDescripcion(detalles.getDescripcion());
            producto.setPrecioCompra(detalles.getPrecioCompra());
            producto.setPrecioVenta(detalles.getPrecioVenta());
            producto.setPrecioMenor(detalles.getPrecioMenor());
            producto.setPrecioMayor(detalles.getPrecioMayor());
            producto.setStock(detalles.getStock());
            producto.setStockMinimo(detalles.getStockMinimo());
            producto.setUnidadMedida(detalles.getUnidadMedida());

            // Solo actualizar importadora si viene en el request
            if (detalles.getImportadora() != null && detalles.getImportadora().getId() != null) {
                producto.setImportadora(detalles.getImportadora());
            }

            // Solo actualizar proveedor si viene en el request
            if (detalles.getProveedor() != null && detalles.getProveedor().getId() != null) {
                producto.setProveedor(detalles.getProveedor());
            }

            Producto productoActualizado = productoRepository.save(producto);
            return ResponseEntity.ok(productoActualizado);

        } catch (DataIntegrityViolationException e) {
            String mensaje = e.getMessage();
            if (mensaje != null && mensaje.contains("codigo_sku")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El código SKU '" + detalles.getCodigoSku() + "' ya existe"));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Violación de integridad: " + mensaje));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar el producto: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            if (!productoRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Producto no encontrado con ID: " + id));
            }

            productoRepository.deleteById(id);
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Producto eliminado correctamente");
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el producto: " + e.getMessage()));
        }
    }
}
