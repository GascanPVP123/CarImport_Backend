package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.CotizacionRequest;
import com.sistemaGestion.api.model.Cotizacion;
import com.sistemaGestion.api.service.CotizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})

public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    // Listar todas las cotizaciones
    @GetMapping
    public ResponseEntity<List<Cotizacion>> listar() {
        return ResponseEntity.ok(cotizacionService.listar());
    }

    // Obtener cotización por ID
    @GetMapping("/{id}")
    public ResponseEntity<Cotizacion> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.obtener(id));
    }

    // Crear cotización (NO descuenta stock)
    @PostMapping
    public ResponseEntity<Cotizacion> crear(@RequestBody CotizacionRequest request) {
        // Obtener usuario autenticado (temporal: "admin")
        String username = "admin";
        return ResponseEntity.ok(cotizacionService.crear(request, username));
    }

    // Actualizar cotización
    @PutMapping("/{id}")
    public ResponseEntity<Cotizacion> actualizar(@PathVariable Long id, @RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(cotizacionService.actualizar(id, request));
    }

    // Cambiar estado
    @PutMapping("/{id}/estado")
    public ResponseEntity<Cotizacion> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(cotizacionService.cambiarEstado(id, estado));
    }

    // Convertir cotización a pedido
    @PostMapping("/{id}/convertir")
    public ResponseEntity<?> convertirAPedido(@PathVariable Long id) {
        Long pedidoId = cotizacionService.convertirAPedido(id);
        return ResponseEntity.ok(new ConvertirResponse(pedidoId));
    }

    // Duplicar cotización
    @PostMapping("/{id}/duplicar")
    public ResponseEntity<Cotizacion> duplicar(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.duplicar(id));
    }

    // Eliminar cotización
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cotizacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // DTO interno
    record ConvertirResponse(Long pedidoId) {}
}