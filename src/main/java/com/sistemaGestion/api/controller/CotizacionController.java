package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.dto.CotizacionRequest;
import com.sistemaGestion.api.model.Cotizacion;
import com.sistemaGestion.api.service.CotizacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping
    public ResponseEntity<List<Cotizacion>> listar() {
        return ResponseEntity.ok(cotizacionService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cotizacion> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CotizacionRequest request) {
        try {
            // ✅ Obtener username del token JWT
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Cotizacion cotizacion = cotizacionService.crear(request, username);
            return ResponseEntity.ok(cotizacion);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cotizacion> actualizar(@PathVariable Long id, @RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(cotizacionService.actualizar(id, request));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Cotizacion> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(cotizacionService.cambiarEstado(id, estado));
    }

    @PostMapping("/{id}/convertir")
    public ResponseEntity<?> convertirAPedido(@PathVariable Long id) {
        Long pedidoId = cotizacionService.convertirAPedido(id);
        return ResponseEntity.ok(new ConvertirResponse(pedidoId));
    }

    @PostMapping("/{id}/duplicar")
    public ResponseEntity<Cotizacion> duplicar(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.duplicar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cotizacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    record ConvertirResponse(Long pedidoId) {}
}