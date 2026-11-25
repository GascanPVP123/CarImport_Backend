package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.model.TiendaAliada;
import com.sistemaGestion.api.repository.TiendaAliadaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tiendas")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class TiendaAliadaController {

    private final TiendaAliadaRepository tiendaAliadaRepository;

    public TiendaAliadaController(TiendaAliadaRepository tiendaAliadaRepository) {
        this.tiendaAliadaRepository = tiendaAliadaRepository;
    }

    @GetMapping
    public ResponseEntity<List<TiendaAliada>> obtenerTodos() {
        try {
            List<TiendaAliada> tiendas = tiendaAliadaRepository.findAll();
            return ResponseEntity.ok(tiendas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/activas")
    public ResponseEntity<List<TiendaAliada>> obtenerActivas() {
        try {
            List<TiendaAliada> tiendas = tiendaAliadaRepository.findByActivoTrue();
            return ResponseEntity.ok(tiendas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return tiendaAliadaRepository.findById(id)
                    .map(tienda -> ResponseEntity.ok((Object) tienda))
                    .orElseGet(() -> {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Tienda no encontrada con ID: " + id);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    });
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener la tienda: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TiendaAliada tienda) {
        try {
            // Validar campos obligatorios
            if (tienda.getNombre() == null || tienda.getNombre().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El nombre de la tienda es obligatorio");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Verificar RUC duplicado
            if (tienda.getRuc() != null && !tienda.getRuc().trim().isEmpty()
                    && tiendaAliadaRepository.existsByRuc(tienda.getRuc())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El RUC '" + tienda.getRuc() + "' ya existe");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            TiendaAliada nuevaTienda = tiendaAliadaRepository.save(tienda);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaTienda);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al crear la tienda: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody TiendaAliada detalles) {
        try {
            // Buscar tienda existente
            TiendaAliada tienda = tiendaAliadaRepository.findById(id).orElse(null);

            if (tienda == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tienda no encontrada con ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Actualizar campos
            if (detalles.getNombre() != null) {
                tienda.setNombre(detalles.getNombre().trim());
            }
            if (detalles.getRuc() != null) {
                tienda.setRuc(detalles.getRuc().trim());
            }
            if (detalles.getDireccion() != null) {
                tienda.setDireccion(detalles.getDireccion().trim());
            }
            if (detalles.getTelefono() != null) {
                tienda.setTelefono(detalles.getTelefono().trim());
            }
            if (detalles.getContacto() != null) {
                tienda.setContacto(detalles.getContacto().trim());
            }
            if (detalles.getEmail() != null) {
                tienda.setEmail(detalles.getEmail().trim());
            }
            if (detalles.getObservaciones() != null) {
                tienda.setObservaciones(detalles.getObservaciones().trim());
            }
            if (detalles.getActivo() != null) {
                tienda.setActivo(detalles.getActivo());
            }

            TiendaAliada tiendaActualizada = tiendaAliadaRepository.save(tienda);
            return ResponseEntity.ok(tiendaActualizada);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al actualizar la tienda: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            if (!tiendaAliadaRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Tienda no encontrada con ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            tiendaAliadaRepository.deleteById(id);

            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Tienda eliminada correctamente");
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al eliminar la tienda: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}