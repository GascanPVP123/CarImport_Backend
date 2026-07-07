package com.sistemaGestion.api.controller;


import com.sistemaGestion.api.model.Producto;
import com.sistemaGestion.api.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    //Lista todos los productos
    @GetMapping
    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAll();
    }

    //Obtiene los productos por id
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductosPorId(@PathVariable Long id) {
        return productoRepository.findById(id)
                .map(producto -> ResponseEntity.ok(producto))
                .orElse(ResponseEntity.notFound().build());
    }

    //Crear un nuevo producto
    @PostMapping
    public Producto crearProducto(@RequestBody Producto producto) {
        return productoRepository.save(producto);
    }

    //Actualizar un producto
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable long id, @RequestBody Producto detallesProducto) {
        return productoRepository.findById(id)
                .map(producto -> {
                    producto.setCodigoSku(detallesProducto.getCodigoSku());
                    producto.setNombre(detallesProducto.getNombre());
                    producto.setImagenUrl(detallesProducto.getImagenUrl());
                    producto.setDescripcion(detallesProducto.getDescripcion());
                    producto.setPrecioCompra(detallesProducto.getPrecioCompra());
                    producto.setPrecioVenta(detallesProducto.getPrecioVenta());
                    producto.setStock(detallesProducto.getStock());
                                        producto.setStockMinimo(detallesProducto.getStockMinimo());
                    producto.setProveedor(detallesProducto.getProveedor());
                    Producto actualizado = productoRepository.save(producto);
                    return ok().body(actualizado);
                }).orElse(ResponseEntity.notFound().build());
    }

    //Borrar un producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    //Graficos
    @GetMapping("/stock-bajo")
    public List<Producto> obtenerStockBajo() {
        return productoRepository.findByStockLessThanEqual(3);
    }

}
