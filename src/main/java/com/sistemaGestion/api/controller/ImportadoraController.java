package com.sistemaGestion.api.controller;

import com.sistemaGestion.api.model.Importadora;
import com.sistemaGestion.api.repository.ImportadoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (importadoraRepository.existsById(id)) {
            importadoraRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}