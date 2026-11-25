package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.model.Importadora;
import com.sistemaGestion.api.repository.ImportadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/importadoras")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class ImportadoraController {

    private final ImportadoraRepository importadoraRepository;

    @GetMapping
    public List<Importadora> listar() {
        return importadoraRepository.findAll();
    }

    @PostMapping
    public Importadora crear(@RequestBody Importadora importadora) {
        return importadoraRepository.save(importadora);
    }

    /**
     * Actualización parcial: solo se modifican los campos enviados (no nulos).
     * 404 si no existe, 400 si ruc/razonSocial quedan vacíos, 409 si el RUC ya lo usa otra importadora.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Importadora detalles) {
        Importadora importadora = importadoraRepository.findById(id).orElse(null);
        if (importadora == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Importadora no encontrada con ID: " + id));
        }

        if (detalles.getRuc() != null) {
            String ruc = detalles.getRuc().trim();
            if (ruc.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El RUC es obligatorio"));
            }
            if (importadoraRepository.existsByRucAndIdNot(ruc, id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El RUC '" + ruc + "' ya está registrado en otra importadora"));
            }
            importadora.setRuc(ruc);
        }
        if (detalles.getRazonSocial() != null) {
            String razonSocial = detalles.getRazonSocial().trim();
            if (razonSocial.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La razón social es obligatoria"));
            }
            importadora.setRazonSocial(razonSocial);
        }
        if (detalles.getTelefono() != null) {
            importadora.setTelefono(detalles.getTelefono().trim());
        }
        if (detalles.getDireccion() != null) {
            importadora.setDireccion(detalles.getDireccion().trim());
        }
        if (detalles.getEmail() != null) {
            importadora.setEmail(detalles.getEmail().trim());
        }

        return ResponseEntity.ok(importadoraRepository.save(importadora));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (importadoraRepository.existsById(id)) {
            importadoraRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
